# Third-Party Notices

## Exercises Dataset

FitTrack includes a transformed, offline seed derived from:

- Project: [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset)
- Copyright: © 2026 Hasan Emir Yıldırım
- Source license: MIT for code, tooling, dataset structure, and instruction text/translations
- Bundled license: `app/src/main/assets/licenses/exercises_dataset_LICENSE.txt`

The build-time preprocessing script keeps 1,324 exercise IDs, names, body parts, equipment types, targets, muscle groups, secondary muscles, and English instruction steps. It removes fields that FitTrack does not use.

The source `LICENSE` and the license bundled in FitTrack have the same SHA-256 digest:

```text
18CAD2F010AB9CC219EE5B11BA0D6BC05D44F3C063A0633ED9FED350AEEA9050
```

### MIT License

Copyright (c) 2026 Hasan Emir Yıldırım

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation and data files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Exercise media exclusion

The upstream dataset also contains thumbnail images and animation GIFs owned by [Gym visual](https://gymvisual.com/). Those media files are governed by separate terms and cloning the dataset does not grant a reuse license.

FitTrack does **not** bundle or display any upstream Gym visual image or GIF. Only the MIT-covered metadata and English instruction text described above are packaged in `exercises_seed.json`.
