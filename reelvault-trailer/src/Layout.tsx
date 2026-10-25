import { Video } from "@remotion/media";
import {
  Easing,
  interpolate,
  staticFile,
  useCurrentFrame,
  useVideoConfig,
} from "remotion";
import { Brand, Footnote, Headline, Phone, Stage } from "./Elements";
import { clamp } from "./theme";
export const Layout = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Stage>
      <Brand />
      <Headline top={215}>
        Your saves.
        <br />
        Your kind of view.
      </Headline>
      <Phone
        style={{
          top: 535,
          scale: interpolate(frame, [0, 0.7 * fps], [1.5, 1], {
            ...clamp,
            easing: Easing.spring({ damping: 20 }),
            output: "perceptual-scale",
          }),
          rotate: interpolate(frame, [0, 0.7 * fps], ["-7deg", "0deg"], clamp),
        }}
      >
        <Video
          name="S23 switches grid to list"
          src={staticFile("clips/layout.mp4")}
          trimBefore={1 * fps}
          premountFor={fps}
          muted
          style={{ width: "100%", height: "100%" }}
        />
      </Phone>
      <Footnote>Big pictures. Or the little details.</Footnote>
    </Stage>
  );
};
