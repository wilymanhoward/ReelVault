import { execFileSync } from "node:child_process";
import { mkdirSync } from "node:fs";
import ffmpeg from "ffmpeg-static";

mkdirSync("public/clips", { recursive: true });
for (const name of ["library", "save", "search", "layout"]) {
  execFileSync(ffmpeg, [
    "-hide_banner",
    "-loglevel",
    "error",
    "-y",
    "-i",
    `public/footage/${name}.mp4`,
    "-vf",
    "fps=30,crop=1080:2180:0:100,tpad=stop_mode=clone:stop_duration=12",
    "-t",
    "12",
    "-an",
    "-c:v",
    "libx264",
    "-preset",
    "fast",
    "-crf",
    "18",
    "-movflags",
    "+faststart",
    `public/clips/${name}.mp4`,
  ]);
}
for (const [name, crop] of [
  ["one", "480:636:48:620"],
  ["two", "480:636:552:620"],
  ["three", "480:540:48:1470"],
]) {
  execFileSync(ffmpeg, [
    "-hide_banner",
    "-loglevel",
    "error",
    "-y",
    "-ss",
    "0.5",
    "-i",
    "public/footage/library.mp4",
    "-frames:v",
    "1",
    "-vf",
    `crop=${crop}`,
    `public/clips/${name}.png`,
  ]);
}
