import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getPaste } from "../api/pastes.js";

function errorMessage(error) {
  if (error.status === 404) return "This paste doesn’t exist or its link is incorrect.";
  if (error.status === 410) return "This paste has expired or reached its view limit.";
  if (error.status >= 500) return `A server error occurred: ${error.message}`;
  return error.message || "Something went wrong while loading this paste.";
}

export default function ViewPastePage() {
  const { id } = useParams();
  const [paste, setPaste] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setPaste(null);

    getPaste(id, { signal: controller.signal })
      .then(setPaste)
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(errorMessage(requestError));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [id]);

  if (loading) {
    return (
      <section className="panel state-panel" aria-live="polite">
        <span className="spinner" aria-hidden="true" />
        <p>Loading your paste…</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className="panel state-panel" role="alert">
        <p className="eyebrow">PASTE UNAVAILABLE</p>
        <h1>{error}</h1>
        <Link className="button button-primary" to="/">Create a new paste</Link>
      </section>
    );
  }

  return (
    <section className="panel view-panel">
      <div className="view-heading">
        <div>
          <p className="eyebrow">SHARED TEXT</p>
          <h1>Your paste</h1>
        </div>
        <Link className="text-link" to="/">Create a paste</Link>
      </div>
      <pre className="paste-content">{paste.content}</pre>
      <div className="view-meta">
        <span>Created {new Date(paste.createdAt).toLocaleString()}</span>
        {paste.viewsRemaining !== null && (
          <span>{paste.viewsRemaining} views remaining</span>
        )}
      </div>
    </section>
  );
}
