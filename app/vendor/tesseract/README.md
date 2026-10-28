# Vendored OCR files (tesseract.js)

The web app loads these files in the browser to read receipts on the user's device
(`app/app.js`, `loadTesseract`). They are unmodified copies of the upstream releases
listed below. They are not covered by Crumina's AGPL-3.0 licence: each keeps its own
licence. Do not edit them; to update, replace them with the files of a new upstream
release, including its `*.LICENSE.txt` files, and update this table and
[`THIRD_PARTY_NOTICES.md`](../../../THIRD_PARTY_NOTICES.md).

| Files | Component | Version | Licence | Upstream |
|---|---|---|---|---|
| `tesseract.min.js`, `worker.min.js` | tesseract.js | 5.1.1 | Apache-2.0 | https://github.com/naptha/tesseract.js (npm `tesseract.js@5.1.1`, `dist/`) |
| `tesseract.min.js.LICENSE.txt`, `worker.min.js.LICENSE.txt` | Upstream notices for packages bundled inside the two files above: regenerator-runtime, buffer, ieee754, zlib.js | from tesseract.js 5.1.1 `dist/` | MIT, BSD-3-Clause | full texts below |
| `tesseract-core.wasm.js`, `tesseract-core-simd.wasm.js`, `tesseract-core-lstm.wasm.js`, `tesseract-core-simd-lstm.wasm.js` | tesseract.js-core (Tesseract OCR and its image libraries compiled to WebAssembly) | 5.1.1 | Apache-2.0, plus the notices in `tesseract-core.wasm.js.LICENSE.txt` | https://github.com/naptha/tesseract.js-core (npm `tesseract.js-core@5.1.1`) |
| `eng.traineddata.gz`, `ind.traineddata.gz` | English and Indonesian language data | npm `@tesseract.js-data/eng@1.0.0` and `@tesseract.js-data/ind@1.0.0`, folder `4.0.0` | Apache-2.0 | https://github.com/naptha/tessdata, built from https://github.com/tesseract-ocr/tessdata (4.0.0) |
| `LICENSE` | Apache License 2.0, full text | | | https://www.apache.org/licenses/LICENSE-2.0.txt |
| `tesseract-core.wasm.js.LICENSE.txt` | Copyright and licence notices for the libraries compiled into the wasm core (Tesseract, Leptonica, libjpeg, giflib, libpng, libtiff, libwebp, zlib, OpenLibm) | | | taken from the submodule commits of tesseract.js-core v5.1.1 |

Provenance was checked on 28 October 2026: the SHA-256 of every `.js` and `.gz` file
here (with line endings as stored in git) matches the file of the same name in the npm
package and version above. The traineddata files match the `4.0.0` folder, not
`4.0.0_best_int`.

The `@tesseract.js-data` npm wrapper packages declare `"license": "MIT"` in their
`package.json`. The language data inside them comes from tesseract-ocr/tessdata, which,
like naptha/tessdata, is licensed under Apache-2.0, so it is listed as Apache-2.0 here.

This software is based in part on the work of the Independent JPEG Group.

## Licence texts for packages bundled in `tesseract.min.js` and `worker.min.js`

The `*.LICENSE.txt` banners name these packages and their licences. The full licence
texts, taken from each package's own repository, are reproduced below.

### regenerator-runtime (in both files)

https://github.com/facebook/regenerator

```text
MIT License

Copyright (c) 2014-present, Facebook, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### buffer (in `worker.min.js`)

https://github.com/feross/buffer

```text
The MIT License (MIT)

Copyright (c) Feross Aboukhadijeh, and other contributors.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

Parts of this software are based on node.js:

Copyright Node.js contributors. All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to
deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.

See https://github.com/nodejs/node/blob/main/LICENSE for more information.
```

### ieee754 (in `worker.min.js`)

https://github.com/feross/ieee754

```text
Copyright 2008 Fair Oaks Labs, Inc.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

### zlib.js (in `worker.min.js`)

https://github.com/imaya/zlib.js

```text
The MIT License

Copyright (c) 2012 imaya

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
```
