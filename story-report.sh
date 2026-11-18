#!/bin/bash
# Runs all tests, builds the HTML reports, and opens the user story report.
# Usage (from the crispy-potato folder):  ./story-report.sh
set -e
cd "$(dirname "$0")"

# if a test fails Maven stops, but we still want the report to show which one
mvn -q clean test surefire-report:report || echo "Some tests failed - building the report anyway..."
python3 docs/make-story-report.py
open docs/user-story-report.html
