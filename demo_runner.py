#!/usr/bin/env python3
"""
demo_runner.py — Sequentially submit all queries from demo-queries.txt to the
                 Spring AI Multi-Model Architecture endpoint /ai/route.

Built for the Multi-Model Architecture video demo. No external dependencies —
pure Python 3 stdlib (urllib, json, argparse).

Usage:
  python3 demo_runner.py                              # default settings
  python3 demo_runner.py --reset                      # reset dashboard first
  python3 demo_runner.py --host http://localhost:8080 --pause 3
  python3 demo_runner.py --queries-file demo-queries.txt
  python3 demo_runner.py --reset --pause 5            # slower for recording

Three typical scenarios for this script:

1. ROUTED DRY-RUN — measure actual cost with the router doing its job:
     python3 demo_runner.py --reset

2. ALL-CLOUD DRY-RUN — measure baseline cost with everything going to Opus:
   First, in QueryRouter.java, hardcode the top of route():
     return new RoutingDecision(ModelTier.CLOUD, "FORCED", excerpt, now);
   Restart Spring Boot, then:
     python3 demo_runner.py --reset
   Restore QueryRouter when done.

3. COLD OPEN VISUAL CAPTURE — recording footage for the intro:
   Same as #2 (all cloud), but with screen recording rolling. Speed-ramp the
   footage to ~10x in post and add a "10x" badge in the corner.

Exit codes:
  0 — All queries submitted (individual queries may still have failed)
  1 — Fatal error (queries file missing, server unreachable, etc.)
"""

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


# --- Output styling --------------------------------------------------------

USE_COLOR = sys.stdout.isatty() and os.environ.get("NO_COLOR") is None


def _wrap(code: str) -> str:
    return code if USE_COLOR else ""


GREEN = _wrap("\033[92m")
RED = _wrap("\033[91m")
YELLOW = _wrap("\033[93m")
CYAN = _wrap("\033[96m")
GRAY = _wrap("\033[90m")
BOLD = _wrap("\033[1m")
RESET = _wrap("\033[0m")


# --- Core logic ------------------------------------------------------------

def parse_queries(content: str):
    """
    Split on lines that contain only '---', then strip the
    'QUERY N — Expected: TIER' header from each chunk.

    Returns list of (header, prompt) tuples.
    """
    chunks = [c.strip() for c in content.split("\n---\n")]
    queries = []
    for chunk in chunks:
        if not chunk:
            continue
        # First line is "QUERY N — Expected: TIER" header; rest is the prompt.
        parts = chunk.split("\n", 1)
        header = parts[0].strip() if parts else ""
        prompt = parts[1].strip() if len(parts) > 1 else ""
        if prompt:
            queries.append((header, prompt))
    return queries


def post_route(host: str, prompt: str, timeout: int):
    """POST a prompt to /ai/route. Returns parsed JSON response or raises."""
    req = urllib.request.Request(
        f"{host}/ai/route",
        data=prompt.encode("utf-8"),
        headers={"Content-Type": "text/plain"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def reset_costs(host: str, timeout: int = 10):
    """POST to /ai/costs/reset to clear dashboard counters."""
    req = urllib.request.Request(f"{host}/ai/costs/reset", method="POST")
    urllib.request.urlopen(req, timeout=timeout).read()


def short_preview(prompt: str, max_len: int = 60) -> str:
    """Get a one-line preview of the prompt for terminal output."""
    first_line = next((line for line in prompt.split("\n") if line.strip()), "")
    return (first_line[:max_len] + "...") if len(first_line) > max_len else first_line


# --- Main ------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Submit demo queries to the Spring AI Multi-Model router endpoint.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--host", default="http://localhost:8080",
                        help="Spring Boot server URL (default: http://localhost:8080)")
    parser.add_argument("--pause", type=float, default=3.0,
                        help="Seconds between queries (default: 3.0)")
    parser.add_argument("--queries-file", default="demo-queries.txt",
                        help="Path to queries file (default: demo-queries.txt)")
    parser.add_argument("--reset", action="store_true",
                        help="Reset dashboard counters before running")
    parser.add_argument("--timeout", type=int, default=300,
                        help="Per-request timeout in seconds (default: 300, for Opus latency)")
    args = parser.parse_args()

    args.host = args.host.rstrip("/")
    queries_path = Path(args.queries_file)

    # --- Validate inputs ---
    if not queries_path.exists():
        print(f"{RED}Error: queries file not found: {queries_path}{RESET}")
        print(f"{GRAY}Hint: run from project root, or pass --queries-file PATH{RESET}")
        sys.exit(1)

    queries = parse_queries(queries_path.read_text(encoding="utf-8"))
    if not queries:
        print(f"{RED}Error: no queries parsed from {queries_path}{RESET}")
        print(f"{GRAY}Expected format: chunks separated by '---' on its own line{RESET}")
        sys.exit(1)

    # --- Header ---
    print(f"{BOLD}{CYAN}Multi-Model Router Demo{RESET}")
    print(f"  Host:            {args.host}")
    print(f"  Queries file:    {queries_path}")
    print(f"  Query count:     {len(queries)}")
    print(f"  Pause between:   {args.pause}s")
    print(f"  Request timeout: {args.timeout}s")
    print()

    # --- Reset if requested ---
    if args.reset:
        print(f"{YELLOW}Resetting dashboard counters...{RESET}")
        try:
            reset_costs(args.host)
            print(f"{GREEN}✓ Counters reset{RESET}\n")
        except urllib.error.URLError as e:
            print(f"{RED}✗ Reset failed: {e.reason}{RESET}")
            print(f"{GRAY}  Is Spring Boot running at {args.host}?{RESET}")
            sys.exit(1)

    # --- Run queries ---
    local_count = 0
    cloud_count = 0
    failed_count = 0
    start_time = time.time()

    for i, (header, prompt) in enumerate(queries, 1):
        print(f"{BOLD}[{i}/{len(queries)}]{RESET} {header}")
        print(f"  {GRAY}→ {short_preview(prompt)}{RESET}")

        try:
            t0 = time.time()
            result = post_route(args.host, prompt, timeout=args.timeout)
            elapsed = time.time() - t0

            decision = result.get("decision", {})
            tier = decision.get("tier", "?")
            rule = decision.get("rule", "?")

            if tier == "LOCAL":
                local_count += 1
                tier_label = f"{GREEN}LOCAL{RESET}"
            elif tier == "CLOUD":
                cloud_count += 1
                tier_label = f"{RED}CLOUD{RESET}"
            else:
                tier_label = f"{YELLOW}{tier}{RESET}"

            print(f"  ✓ {tier_label}  ({rule})  {GRAY}{elapsed:.1f}s{RESET}")

        except urllib.error.HTTPError as e:
            failed_count += 1
            print(f"  {RED}✗ HTTP {e.code} {e.reason}{RESET}")
            try:
                body = e.read().decode("utf-8")
                print(f"  {GRAY}  {body[:200]}{RESET}")
            except Exception:
                pass
        except urllib.error.URLError as e:
            failed_count += 1
            print(f"  {RED}✗ Connection error: {e.reason}{RESET}")
            print(f"  {GRAY}  Is Spring Boot running at {args.host}?{RESET}")
            sys.exit(1)
        except Exception as e:
            failed_count += 1
            print(f"  {RED}✗ {type(e).__name__}: {e}{RESET}")

        # Pause between queries (skip after the last one)
        if i < len(queries):
            time.sleep(args.pause)
        print()

    total_elapsed = time.time() - start_time

    # --- Summary ---
    print(f"{BOLD}{CYAN}{'─' * 60}{RESET}")
    print(f"{BOLD}Demo complete{RESET}")
    print(f"  Total queries:  {len(queries)}")
    print(f"  {GREEN}LOCAL:          {local_count}{RESET}")
    print(f"  {RED}CLOUD:          {cloud_count}{RESET}")
    if failed_count:
        print(f"  {RED}FAILED:         {failed_count}{RESET}")
    print(f"  Total time:     {total_elapsed:.1f}s")
    print()
    print(f"{YELLOW}→ Open the dashboard at {args.host}/dashboard.html for cost breakdown{RESET}")


if __name__ == "__main__":
    main()
