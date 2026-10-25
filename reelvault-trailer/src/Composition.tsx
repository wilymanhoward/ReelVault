import { Audio } from "@remotion/media";
import {
  AbsoluteFill,
  Composition,
  Folder,
  Series,
  staticFile,
  useVideoConfig, Track,
} from "remotion";
import { Opening } from "./Opening";
import { Library } from "./Library";
import { Save } from "./Save";
import { Search } from "./Search";
import { Layout } from "./Layout";
import { Closing } from "./Closing";
import { palette } from "./theme";
export const ReelVaultTrailer = () => {
  const { fps } = useVideoConfig();
  return (
    <>
      <AbsoluteFill style={{ background: palette.ground }}>
        <Series>
          <Series.Sequence
            name="Good videos. Bad memory?"
            durationInFrames={3 * fps}
            premountFor={fps}
          >
            <Opening />
          </Series.Sequence>
          <Series.Sequence
            name="A home for your saves"
            durationInFrames={5 * fps}
            premountFor={fps}
          >
            <Library />
          </Series.Sequence>
          <Series.Sequence
            name="Save a link"
            durationInFrames={4 * fps}
            premountFor={fps}
          >
            <Save />
          </Series.Sequence>
          <Series.Sequence
            name="Find it again"
            durationInFrames={5 * fps}
            premountFor={fps}
          >
            <Search />
          </Series.Sequence>
          <Series.Sequence
            name="Switch your view"
            durationInFrames={3 * fps}
            premountFor={fps}
          >
            <Layout />
          </Series.Sequence>
          <Series.Sequence
            name="Get ReelVault"
            durationInFrames={4 * fps}
            premountFor={fps}
          >
            <Closing />
          </Series.Sequence>
        </Series>
      </AbsoluteFill>
      <Track name="ALICE - THAT GIRL.mp3">
        <Audio
          src={staticFile("ALICE - THAT GIRL.mp3")}
          durationInFrames={275}
          toneFrequency={0.7}
        />
        <Audio
          src={staticFile("ALICE - THAT GIRL.mp3")}
          from={275}
          durationInFrames={444}
          trimBefore={275}
          volume={0.61}
        />
      </Track>
    </>
  );
};
export const MyComposition = () => (
  <>
    <Composition
      id="ReelVault-Trailer"
      component={ReelVaultTrailer}
      durationInFrames={720}
      fps={30}
      width={1080}
      height={1920}
    />
    <Folder name="Scenes">
      <Composition
        id="Opening"
        component={Opening}
        durationInFrames={90}
        fps={30}
        width={1080}
        height={1920}
      />
      <Composition
        id="Library"
        component={Library}
        durationInFrames={150}
        fps={30}
        width={1080}
        height={1920}
      />
      <Composition
        id="Save"
        component={Save}
        durationInFrames={120}
        fps={30}
        width={1080}
        height={1920}
      />
      <Composition
        id="Search"
        component={Search}
        durationInFrames={150}
        fps={30}
        width={1080}
        height={1920}
      />
      <Composition
        id="Layout"
        component={Layout}
        durationInFrames={90}
        fps={30}
        width={1080}
        height={1920}
      />
      <Composition
        id="Closing"
        component={Closing}
        durationInFrames={120}
        fps={30}
        width={1080}
        height={1920}
      />
    </Folder>
  </>
);
