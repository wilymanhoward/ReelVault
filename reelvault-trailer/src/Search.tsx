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
export const Search = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return (
    <Stage>
      <Brand />
      <Headline top={215}>
        Oh, that video.
        <br />
        Found it.
      </Headline>
      <Phone
        style={{
          top: 535,
          rotate: interpolate(
            frame,
            [0, 0.8 * fps, 5 * fps],
            ["9deg", "2deg", "-1deg"],
            clamp,
          ),
          translate: interpolate(
            frame,
            [0, 0.75 * fps],
            ["-900px 0px", "0px 0px"],
            { ...clamp, easing: Easing.spring({ damping: 19 }) },
          ),
        }}
      >
        <Video
          name="S23 searches Claude"
          src={staticFile("clips/search.mp4")}
          trimBefore={2 * fps}
          premountFor={fps}
          muted
          style={{ width: "100%", height: "100%" }}
        />
      </Phone>
      <Footnote>A few letters. Back in your world.</Footnote>
    </Stage>
  );
};
