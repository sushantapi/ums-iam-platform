import { StatusBadge } from "../../components/ui/StatusBadge";
import type { ScreenBlueprint } from "./screenBlueprints";

export function BlueprintPage({ screen }: { screen: ScreenBlueprint }) {
  const Icon = screen.icon;

  return (
    <section className="page">
      <div className="page-header">
        <div>
          <div className="eyebrow">{screen.section}</div>
          <h1>
            <Icon size={28} />
            {screen.title}
          </h1>
          <p>{screen.summary}</p>
        </div>
        <StatusBadge status={screen.status} />
      </div>

      <div className="blueprint-grid">
        <section className="panel">
          <h2>Primary Actions</h2>
          <div className="action-row">
            {screen.primaryActions.map((action) => (
              <button key={action} className="button-secondary">
                {action}
              </button>
            ))}
          </div>
        </section>
        <section className="panel">
          <h2>Data Sources</h2>
          <ul className="detail-list">
            {screen.dataSources.map((source) => (
              <li key={source}>{source}</li>
            ))}
          </ul>
        </section>
        <section className="panel panel-wide">
          <h2>Screen Composition</h2>
          <div className="widget-grid">
            {screen.widgets.map((widget) => (
              <div key={widget} className="widget-tile">
                {widget}
              </div>
            ))}
          </div>
        </section>
      </div>
    </section>
  );
}
