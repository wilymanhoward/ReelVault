# ReelVault trailer

24-second portrait trailer (1080 x 1920, 30 fps) made with Remotion.

## Preview

Run `npm install`, then `npm run dev -- --no-open`. Open the printed Studio address and choose `ReelVault-Trailer`. Each scene also has its own editable composition.

## Footage and design

- Original app recordings were captured from the connected Samsung Galaxy S23 on 2026-10-24, using the installed ReelVault app and its real saved-video library.
- `public/footage/` contains the source recordings; `public/clips/` contains cropped, constant-frame-rate copies. The crop removes the system status and gesture bars. Static recording tails are held, not replaced with invented app interactions.
- `scripts/prepare-footage.mjs` regenerates the prepared clips and opening thumbnails. Run it from this directory with `node scripts/prepare-footage.mjs`.
- Inter and Source Serif 4 are copied from the Android app, alongside their OFL licenses. The icon is the project's original ReelVault SVG.
- The palette matches the app: #101010, #191919, #262625 and #F7F6F2.
- The 120 BPM soundtrack is an original synthesized cue created locally with `scripts/make-score.py` (Python + NumPy). The footage is muted; no third-party music is used.
- Thumbnails visible in the real app remain their respective creators' content. The trailer demonstrates the app as recorded.

## Timeline

0–3s: opening thumbnail collage. 3–8s: library scroll. 8–12s: add-link sheet. 12–17s: search. 17–20s: grid/list switch. 20–24s: app and website end card.

## Checks and export

`npm run lint` checks TypeScript and ESLint. Review stills can be generated with `npx remotion render ReelVault-Trailer out/review --frames=60,150,300,465,555,690 --image-format=png --port=3101`.

The editable Studio preview is the current deliverable. To export an MP4, use Studio's Render button or `npx remotion render ReelVault-Trailer out/reelvault-trailer.mp4 --port=3101`.
