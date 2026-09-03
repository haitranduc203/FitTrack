import importlib.util
import pathlib
import unittest

ROOT_TEST = pathlib.Path(__file__).resolve().parent.parent / "test_prepare_exercise_seed.py"
spec = importlib.util.spec_from_file_location("root_test_prepare_exercise_seed", ROOT_TEST)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

TestPrepareExerciseSeed = module.TestPrepareExerciseSeed

if __name__ == "__main__":
    unittest.main()
