import {
  CanvasImage,
  Easing,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Stage } from "./Elements";
import { clamp, palette, serif } from "./theme";
export const Closing = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Stage>
      <Interactive.Div
        name="App icon landing"
        premountFor={fps}
        style={{
          position: "absolute",
          top: 300,
          left: 390,
          width: 300,
          height: 300,
          rotate: interpolate(frame, [0, 0.9 * fps], ["-25deg", "0deg"], {
            ...clamp,
            easing: Easing.spring({ damping: 13 }),
          }),
          scale: interpolate(frame, [0, 0.9 * fps], [0.2, 1], {
            ...clamp,
            easing: Easing.spring({ damping: 13 }),
            output: "perceptual-scale",
          }),
        }}
      >
        <CanvasImage
          src={staticFile("icon.svg")}
          premountFor={fps}
          style={{ width: "100%", height: "100%" }}
        />
      </Interactive.Div>
      <Interactive.Div
        name="ReelVault title"
        premountFor={fps}
        style={{
          position: "absolute",
          top: 650,
          left: 84,
          right: 84,
          textAlign: "center",
          fontFamily: serif,
          fontSize: 154,
          fontWeight: 600,
          letterSpacing: -5,
          opacity: interpolate(frame, [0.2 * fps, 0.7 * fps], [0, 1], clamp),
          translate: interpolate(
            frame,
            [0.2 * fps, 0.8 * fps],
            ["0px 70px", "0px 0px"],
            { ...clamp, easing: Easing.spring({ damping: 18 }) },
          ),
        }}
      >
        ReelVault
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          top: 900,
          left: 84,
          right: 84,
          textAlign: "center",
          fontFamily: serif,
          fontSize: 96,
          lineHeight: 1.15,
          letterSpacing: -2,
          opacity: interpolate(frame, [0.6 * fps, 1 * fps], [0, 1], clamp),
        }}
      >
        Save it.
        <br />
        Find it again.
      </div>
      <Interactive.Div
        name="Website address"
        premountFor={fps}
        style={{
          position: "absolute",
          top: 1280,
          left: 160,
          right: 160,
          padding: "34px 20px",
          borderRadius: 30,
          background: palette.paper,
          color: palette.ground,
          textAlign: "center",
          fontSize: 52,
          fontWeight: 600,
          letterSpacing: -2,
          scale: interpolate(frame, [1 * fps, 1.65 * fps], [0.7, 1], {
            ...clamp,
            easing: Easing.spring({ damping: 13 }),
            output: "perceptual-scale",
          }),
          opacity: interpolate(frame, [1 * fps, 1.2 * fps], [0, 1], clamp),
        }}
      >
        getreelvault.web.app
      </Interactive.Div>
      <div
        style={{
          position: "absolute",
          top: 1510,
          left: 84,
          right: 84,
          textAlign: "center",
          fontSize: 44,
          color: palette.muted,
          opacity: interpolate(frame, [1.5 * fps, 1.9 * fps], [0, 1], clamp),
        }}
      >
        Made for Android. Open source.
      </div>
    </Stage>
  );
};
