import { useLocation, useNavigate } from "react-router";

export function ModuleTabs({ tabs }: { tabs: [string, string][] }) {
  const navigate = useNavigate();
  const location = useLocation();
  return (
    <div className="mb-5 flex gap-0.5 overflow-x-auto border-b border-border pb-px">
      {tabs.map(([path, label]) => (
        <button
          key={path}
          onClick={() => navigate(path)}
          className={`whitespace-nowrap px-3 py-2 text-sm transition-all ${location.pathname === path ? "-mb-px border-b-2 border-[var(--byop-violet)] font-medium text-foreground" : "text-muted-foreground hover:text-foreground"}`}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
