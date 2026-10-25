import { Video } from "@remotion/media";
import {
  Easing,
  Interactive,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Brand, Footnote, Headline, Stage } from "./Elements";
import { clamp, palette } from "./theme";
export const Save = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Stage light>
      <Brand light />
      <Headline light top={265} size={116}>
        One link.
        <br />
        Yours for later.
      </Headline>
      <Interactive.Div
        name="Save sheet close-up"
        premountFor={fps}
        style={{
          position: "absolute",
          left: 84,
          right: 84,
          top: 690,
          height: 870,
          borderRadius: 48,
          overflow: "hidden",
          background: palette.ground,
          boxShadow: "0 28px 60px #24232122",
          rotate: interpolate(frame, [0, 0.8 * fps], ["5deg", "0deg"], clamp),
          translate: interpolate(
            frame,
            [0, 0.7 * fps],
            ["900px 150px", "0px 0px"],
            { ...clamp, easing: Easing.spring({ damping: 18 }) },
          ),
        }}
      >
        <Video
          name="S23 opens add-link sheet"
          src={staticFile("clips/save.mp4")}
          trimBefore={1 * fps}
          premountFor={fps}
          muted
          style={{
            position: "absolute",
            bottom: 0,
            width: "100%",
            height: "auto",
          }}
        />
      </Interactive.Div>
      <Footnote light>Paste it. Keep it. Come back to it.</Footnote>
    </Stage>
  );
};
