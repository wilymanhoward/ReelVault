import {
  CanvasImage,
  Easing,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Brand, Footnote, Headline, Stage } from "./Elements";
import { clamp, palette, serif } from "./theme";
export const Opening = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Stage>
      <Brand />
      <Headline top={285} size={116}>
        Good videos.
      </Headline>
      <Interactive.Div
        name="Bad memory?"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 84,
          top: 425,
          fontFamily: serif,
          fontSize: 116,
          fontWeight: 600,
          letterSpacing: -4,
          opacity: interpolate(frame, [0.3 * fps, 0.5 * fps], [0, 1], clamp),
          translate: interpolate(
            frame,
            [0.3 * fps, 1 * fps],
            ["0px 80px", "0px 0px"],
            { ...clamp, easing: Easing.spring({ damping: 12 }) },
          ),
        }}
      >
        Bad memory?
      </Interactive.Div>
      <Interactive.Div
        name="Saved clip one"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 90,
          top: 810,
          width: 350,
          height: 510,
          overflow: "hidden",
          borderRadius: 34,
          rotate: interpolate(frame, [0, 1 * fps], ["-24deg", "-12deg"], clamp),
          translate: interpolate(
            frame,
            [0, 0.85 * fps],
            ["-550px 200px", "0px 0px"],
            { ...clamp, easing: Easing.spring({ damping: 15 }) },
          ),
        }}
      >
        <CanvasImage
          src={staticFile("clips/one.png")}
          premountFor={fps}
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
      </Interactive.Div>
      <Interactive.Div
        name="Saved clip three"
        premountFor={fps}
        style={{
          position: "absolute",
          right: 90,
          top: 870,
          width: 350,
          height: 510,
          overflow: "hidden",
          borderRadius: 34,
          rotate: interpolate(frame, [0, 1 * fps], ["24deg", "12deg"], clamp),
          translate: interpolate(
            frame,
            [0.15 * fps, 1.1 * fps],
            ["550px 200px", "0px 0px"],
            { ...clamp, easing: Easing.spring({ damping: 15 }) },
          ),
        }}
      >
        <CanvasImage
          src={staticFile("clips/three.png")}
          premountFor={fps}
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
      </Interactive.Div>
      <Interactive.Div
        name="Saved clip two"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 350,
          top: 945,
          width: 380,
          height: 510,
          overflow: "hidden",
          borderRadius: 34,
          boxShadow: "0 20px 60px #0009",
          rotate: interpolate(frame, [0, 1.2 * fps], ["8deg", "-2deg"], clamp),
          scale: interpolate(frame, [0.35 * fps, 1.1 * fps], [0.1, 1], {
            ...clamp,
            easing: Easing.spring({ damping: 14 }),
            output: "perceptual-scale",
          }),
        }}
      >
        <CanvasImage
          src={staticFile("clips/two.png")}
          premountFor={fps}
          style={{ width: "100%", height: "100%", objectFit: "cover" }}
        />
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          top: 1540,
          left: 84,
          right: 84,
          textAlign: "center",
          fontFamily: serif,
          fontSize: 65,
          color: palette.paper,
          opacity: interpolate(frame, [1.5 * fps, 1.9 * fps], [0, 1], clamp),
        }}
      >
        Give them a home.
      </div>
      <Footnote>Your next rabbit hole starts here.</Footnote>
    </Stage>
  );
};
