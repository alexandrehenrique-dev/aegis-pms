import { useContext } from "react";
import { TutorialContext } from "./tutorialContextDefinition";

export const useTutorial = () => useContext(TutorialContext);
