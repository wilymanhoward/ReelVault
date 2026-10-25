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
export const Library = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Stage>
      <Brand />
      <Headline top={215}>
        All your saves.
        <br />
        One happy place.
      </Headline>
      <Phone
        style={{
          top: 535,
          rotate: interpolate(
            frame,
            [0, 1 * fps, 5 * fps],
            ["-12deg", "-3deg", "2deg"],
            clamp,
          ),
          translate: interpolate(
            frame,
            [0, 0.8 * fps],
            ["0px 1100px", "0px 0px"],
            { ...clamp, easing: Easing.spring({ damping: 18 }) },
          ),
          scale: interpolate(frame, [0, 1 * fps, 5 * fps], [0.85, 1, 1.025], {
            ...clamp,
            output: "perceptual-scale",
          }),
        }}
      >
        <Video
          name="S23 library scroll"
          src={staticFile("clips/library.mp4")}
          trimBefore={0.5 * fps}
          premountFor={fps}
          muted
          style={{ width: "100%", height: "100%" }}
        />
      </Phone>
      <Footnote>Less hunting. More watching.</Footnote>
    </Stage>
  );
};
