import json
import pathlib
import subprocess
import sys
import tempfile
import unittest

PREPARE_SCRIPT = pathlib.Path(__file__).resolve().parent / "prepare_exercise_seed.py"
if not PREPARE_SCRIPT.exists():
    PREPARE_SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "prepare_exercise_seed.py"

SAMPLE_RECORD_1 = {
    "id": "ex_002",
    "name": "Barbell Squat",
    "body_part": "Legs",
    "equipment": "Barbell",
    "target": "Quadriceps",
    "muscle_group": "Legs",
    "secondary_muscles": ["Glutes", "Hamstrings"],
    "instruction_steps": {
        "en": ["Step 1: Stand with feet shoulder-width apart.", "Step 2: Squat down until thighs are parallel."]
    },
    "image": "images/ex_002.jpg",
    "gif_url": "videos/ex_002.gif",
    "media_id": "media_002",
    "attribution": "Dataset Creator"
}

SAMPLE_RECORD_2 = {
    "id": "ex_001",
    "name": "Bench Press",
    "body_part": "Chest",
    "equipment": "Barbell",
    "target": "Pectorals",
    "muscle_group": "Chest",
    "secondary_muscles": ["Triceps"],
    "instruction_steps": {
        "en": ["Step 1: Lie on bench.", "Step 2: Press bar up."]
    },
    "image": "images/ex_001.jpg",
    "gif_url": "videos/ex_001.gif",
    "media_id": "media_001",
    "attribution": "Dataset Creator"
}

class TestPrepareExerciseSeed(unittest.TestCase):
    def run_script(self, input_path: pathlib.Path, output_path: pathlib.Path) -> subprocess.CompletedProcess:
        return subprocess.run(
            [sys.executable, str(PREPARE_SCRIPT), "--input", str(input_path), "--output", str(output_path)],
            capture_output=True,
            text=True
        )

    def test_valid_records_transformation_and_sorting(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            input_file = pathlib.Path(tmpdir) / "input.json"
            output_file = pathlib.Path(tmpdir) / "output.json"

            input_file.write_text(json.dumps([SAMPLE_RECORD_1, SAMPLE_RECORD_2]), encoding="utf-8")

            result = self.run_script(input_file, output_file)
            self.assertEqual(result.returncode, 0, msg=result.stderr)
            self.assertEqual(result.stdout.strip(), "2")

            output = json.loads(output_file.read_text(encoding="utf-8"))
            self.assertEqual(len(output), 2)
            # Must be sorted by ID: ex_001 then ex_002
            self.assertEqual(output[0]["id"], "ex_001")
            self.assertEqual(output[1]["id"], "ex_002")

            # Check exact keys in order
            expected_keys = [
                "id", "name", "bodyPart", "equipment", "target",
                "muscleGroup", "secondaryMuscles", "instructions"
            ]
            self.assertEqual(list(output[0].keys()), expected_keys)
            self.assertEqual(output[0]["bodyPart"], "Chest")
            self.assertEqual(output[0]["muscleGroup"], "Chest")
            self.assertEqual(output[0]["secondaryMuscles"], ["Triceps"])
            self.assertEqual(output[0]["instructions"], ["Step 1: Lie on bench.", "Step 2: Press bar up."])

            # Media keys and paths must be absent
            serialized = json.dumps(output)
            self.assertNotIn("image", serialized)
            self.assertNotIn("gif_url", serialized)
            self.assertNotIn("media_id", serialized)
            self.assertNotIn("attribution", serialized)
            self.assertNotIn("images/", serialized)
            self.assertNotIn("videos/", serialized)

    def test_duplicate_id_fails(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            input_file = pathlib.Path(tmpdir) / "input.json"
            output_file = pathlib.Path(tmpdir) / "output.json"

            dup_record = dict(SAMPLE_RECORD_1)
            dup_record["id"] = "ex_001"
            input_file.write_text(json.dumps([SAMPLE_RECORD_2, dup_record]), encoding="utf-8")

            result = self.run_script(input_file, output_file)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("duplicate", result.stderr.lower())

    def test_missing_instruction_steps_en_fails(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            input_file = pathlib.Path(tmpdir) / "input.json"
            output_file = pathlib.Path(tmpdir) / "output.json"

            bad_record = dict(SAMPLE_RECORD_1)
            bad_record["instruction_steps"] = {}
            input_file.write_text(json.dumps([bad_record]), encoding="utf-8")

            result = self.run_script(input_file, output_file)
            self.assertNotEqual(result.returncode, 0)

    def test_blank_instruction_fails(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            input_file = pathlib.Path(tmpdir) / "input.json"
            output_file = pathlib.Path(tmpdir) / "output.json"

            bad_record = dict(SAMPLE_RECORD_1)
            bad_record["instruction_steps"] = {"en": ["   "]}
            input_file.write_text(json.dumps([bad_record]), encoding="utf-8")

            result = self.run_script(input_file, output_file)
            self.assertNotEqual(result.returncode, 0)

    def test_malformed_json_fails(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            input_file = pathlib.Path(tmpdir) / "input.json"
            output_file = pathlib.Path(tmpdir) / "output.json"

            input_file.write_text("{ not valid json ]", encoding="utf-8")

            result = self.run_script(input_file, output_file)
            self.assertNotEqual(result.returncode, 0)

    def test_missing_input_file_fails(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            input_file = pathlib.Path(tmpdir) / "non_existent.json"
            output_file = pathlib.Path(tmpdir) / "output.json"

            result = self.run_script(input_file, output_file)
            self.assertNotEqual(result.returncode, 0)

if __name__ == "__main__":
    unittest.main()
