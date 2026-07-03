import { motion } from "motion/react";
import { fade } from "../../shared/components/motion";

export function ScreenSkeleton() {
  return (
    <motion.div {...fade} className="space-y-5">
      <div className="space-y-2">
        <div className="h-7 w-52 animate-pulse rounded-xl bg-muted" />
        <div className="h-4 w-80 animate-pulse rounded-lg bg-muted" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[1, 2, 3, 4].map((i) => <div key={i} className="h-28 animate-pulse rounded-2xl bg-muted" />)}
      </div>
      <div className="grid gap-4 xl:grid-cols-[1fr_360px]">
        <div className="space-y-4">
          <div className="h-64 animate-pulse rounded-2xl bg-muted" />
          <div className="h-32 animate-pulse rounded-2xl bg-muted" />
        </div>
        <div className="h-80 animate-pulse rounded-2xl bg-muted" />
      </div>
    </motion.div>
  );
}
