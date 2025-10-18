from __future__ import annotations

import os
from celery import Celery
from src.middlewares.correlation import set_current_correlation_id

# Basic Celery app example (configure broker URL in env: CELERY_BROKER_URL)
celery = Celery("summarizer")


@celery.task(bind=True)
def run_pipeline_task(self, job_id: str, correlation_id: str | None = None, top: int = 25):
    """Example Celery task that restores correlation id into context and runs pipeline.

    The caller should set correlation_id when enqueuing the job so logs and traces
    can link back to the originating request.
    """
    # restore correlation id in this worker context
    if correlation_id:
        set_current_correlation_id(correlation_id)

    # Optionally set env var for subprocesses
    if correlation_id:
        os.environ["X_CORRELATION_ID"] = correlation_id

    # import and run pipeline
    from src.pipelines.news_pipeline import run_pipeline

    result_path = run_pipeline(top_n=top)
    return str(result_path)
