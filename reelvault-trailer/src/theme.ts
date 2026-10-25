import { loadFont } from "@remotion/fonts";
import { staticFile } from "remotion";
loadFont({
  family: "Inter",
  url: staticFile("fonts/inter.ttf"),
  weight: "100 900",
});
loadFont({
  family: "Source Serif 4",
  url: staticFile("fonts/source-serif-4.ttf"),
  weight: "200 900",
});
export const palette = {
  ground: "#101010",
  surface: "#191919",
  raised: "#262625",
  paper: "#F7F6F2",
  muted: "#AAA7A1",
};
export const serif = '"Source Serif 4", Georgia, serif';
export const sans = "Inter, sans-serif";
export const clamp = {
  extrapolateLeft: "clamp",
  extrapolateRight: "clamp",
} as const;
