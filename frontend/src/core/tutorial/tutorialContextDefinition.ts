import { createContext } from "react";

export type TutorialContextValue = {
  startTutorial: () => void;
  stopTutorial: () => void;
  isRunning: boolean;
};

export const TutorialContext = createContext<TutorialContextValue>({
  startTutorial: () => {},
  stopTutorial: () => {},
  isRunning: false,
});
