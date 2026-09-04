import argparse
import json
import pathlib
import sys

OUTPUT_KEYS = (
    "id", "name", "bodyPart", "equipment", "target",
    "muscleGroup", "secondaryMuscles", "instructions",
)

def require_text(record: dict, key: str) -> str:
    value = record.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"invalid {key}")
    return value.strip()

def transform_record(source: dict) -> dict:
    steps_map = source.get("instruction_steps")
    steps = steps_map.get("en") if isinstance(steps_map, dict) else None
    if not isinstance(steps, list) or not steps or any(not isinstance(step, str) or not step.strip() for step in steps):
        raise ValueError("invalid instruction_steps.en")
    secondary = source.get("secondary_muscles")
    if not isinstance(secondary, list) or any(not isinstance(item, str) or not item.strip() for item in secondary):
        raise ValueError("invalid secondary_muscles")
    return {
        "id": require_text(source, "id"),
        "name": require_text(source, "name"),
        "bodyPart": require_text(source, "body_part"),
        "equipment": require_text(source, "equipment"),
        "target": require_text(source, "target"),
        "muscleGroup": require_text(source, "muscle_group"),
        "secondaryMuscles": [item.strip() for item in secondary],
        "instructions": [step.strip() for step in steps],
    }

def prepare_seed(input_path: pathlib.Path, output_path: pathlib.Path) -> int:
    source = json.loads(input_path.read_text(encoding="utf-8"))
    if not isinstance(source, list):
        raise ValueError("root must be an array")
    records = [transform_record(item) for item in source]
    ids = [record["id"] for record in records]
    if len(ids) != len(set(ids)):
        raise ValueError("duplicate exercise id")
    records.sort(key=lambda item: item["id"])
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(records)

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args(argv)
    try:
        count = prepare_seed(args.input, args.output)
    except (OSError, json.JSONDecodeError, KeyError, TypeError, ValueError) as error:
        print(f"seed generation failed: {error}", file=sys.stderr)
        return 1
    print(count)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
