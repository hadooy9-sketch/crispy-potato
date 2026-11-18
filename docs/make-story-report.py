"""
Builds docs/user-story-report.html: a one-page report of the user story tests.

Run from the crispy-potato folder, AFTER the tests:
    mvn clean test
    python3 docs/make-story-report.py

Pass/fail comes from Surefire's XML files in target/surefire-reports.
The plain-English names come from the @DisplayName lines in UserStoryAcceptanceTest.java.
"""

import glob
import html
import os
import re
import xml.etree.ElementTree as ET
from datetime import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEST_FILE = os.path.join(ROOT, "src/test/java/qasystem/UserStoryAcceptanceTest.java")
RESULTS = os.path.join(ROOT, "target/surefire-reports")
OUT = os.path.join(ROOT, "docs/user-story-report.html")


def read_display_names():
    """Returns {nested class: story title} and {(class, method): sentence}."""
    stories, sentences = {}, {}
    pending = None   # the last @DisplayName we saw, waiting for its class/method
    current = None   # the nested class we're inside
    for line in open(TEST_FILE, encoding="utf-8"):
        m = re.search(r'@DisplayName\("(.*)"\)', line)
        if m:
            pending = m.group(1).replace('\\"', '"')
            continue
        m = re.search(r'\bclass (\w+)', line)
        if m and pending:
            current = m.group(1)
            stories[current] = pending
            pending = None
            continue
        m = re.search(r'\bvoid (\w+)\(', line)
        if m and pending and current:
            sentences[(current, m.group(1))] = pending
            pending = None
    return stories, sentences


def read_results():
    """Returns {(class, method): 'pass' | 'fail' | 'skipped'}."""
    results = {}
    for path in glob.glob(os.path.join(RESULTS, "TEST-qasystem.UserStoryAcceptanceTest$*.xml")):
        for case in ET.parse(path).getroot().iter("testcase"):
            cls = case.get("classname").split("$")[-1]
            if case.find("failure") is not None or case.find("error") is not None:
                status = "fail"
            elif case.find("skipped") is not None:
                status = "skipped"
            else:
                status = "pass"
            results[(cls, case.get("name"))] = status
    return results


def build_page(stories, sentences, results):
    # stories that are actually user stories, in US-01, US-02... order
    order = sorted((t, c) for c, t in stories.items() if t.startswith("US-"))
    total = sum(1 for k in sentences if k in results)
    passed = sum(1 for k in sentences if results.get(k) == "pass")
    all_ok = total > 0 and passed == total

    cards = []
    for title, cls in order:
        tests = [(m, s) for (c, m), s in sentences.items() if c == cls]
        rows, ok = [], 0
        for method, sentence in tests:
            status = results.get((cls, method), "not run")
            ok += status == "pass"
            icon = {"pass": "&#10003;", "fail": "&#10007;"}.get(status, "&ndash;")
            rows.append(f'<li class="{status.replace(" ", "-")}"><span class="icon">{icon}</span>'
                        f'{html.escape(sentence[0].upper() + sentence[1:])}</li>')
        badge = "pass" if ok == len(tests) else "fail"
        code, _, story = title.partition(": ")
        cards.append(f"""
    <section class="card">
      <div class="card-head">
        <div><span class="code">{html.escape(code)}</span><h2>{html.escape(story)}</h2></div>
        <span class="badge {badge}">{ok}/{len(tests)} passed</span>
      </div>
      <ul>{''.join(rows)}</ul>
    </section>""")

    when = datetime.fromtimestamp(os.path.getmtime(RESULTS)).strftime("%B %d, %Y at %I:%M %p")
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>User Story Test Report</title>
<style>
  :root {{ --ink:#1f2328; --quiet:#59636e; --line:#d1d9e0; --bg:#f6f8fa;
          --good:#1a7f37; --good-bg:#dafbe1; --bad:#cf222e; --bad-bg:#ffebe9; }}
  * {{ box-sizing:border-box; }}
  body {{ margin:0; background:var(--bg); color:var(--ink);
         font:15px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; }}
  main {{ max-width:860px; margin:0 auto; padding:32px 16px 48px; }}
  h1 {{ margin:0; font-size:28px; }}
  .sub {{ color:var(--quiet); margin:4px 0 24px; }}
  .banner {{ display:flex; align-items:center; gap:16px; padding:20px 24px; border-radius:12px;
            margin-bottom:24px; border:1px solid; }}
  .banner.pass {{ background:var(--good-bg); border-color:var(--good); }}
  .banner.fail {{ background:var(--bad-bg); border-color:var(--bad); }}
  .banner .big {{ font-size:40px; font-weight:700; line-height:1; }}
  .banner.pass .big {{ color:var(--good); }}  .banner.fail .big {{ color:var(--bad); }}
  .banner p {{ margin:0; }}
  .card {{ background:#fff; border:1px solid var(--line); border-radius:12px; padding:20px 24px;
          margin-bottom:16px; break-inside:avoid; }}
  .card-head {{ display:flex; justify-content:space-between; align-items:flex-start; gap:12px; }}
  .code {{ font-size:12px; font-weight:700; color:var(--quiet); letter-spacing:.05em; }}
  h2 {{ margin:2px 0 0; font-size:18px; }}
  .badge {{ white-space:nowrap; font-size:13px; font-weight:600; padding:4px 10px; border-radius:999px; }}
  .badge.pass {{ background:var(--good-bg); color:var(--good); }}
  .badge.fail {{ background:var(--bad-bg); color:var(--bad); }}
  ul {{ list-style:none; margin:14px 0 0; padding:0; }}
  li {{ display:flex; gap:10px; padding:7px 0; border-top:1px solid var(--bg); }}
  .icon {{ font-weight:700; width:18px; flex:none; }}
  li.pass .icon {{ color:var(--good); }}  li.fail .icon {{ color:var(--bad); }}
  li.fail {{ color:var(--bad); }}  li.skipped, li.not-run {{ color:var(--quiet); }}
  footer {{ color:var(--quiet); font-size:13px; margin-top:24px; }}
  code {{ background:#eef1f4; padding:1px 5px; border-radius:4px; }}
  @media print {{ body {{ background:#fff; }} main {{ padding-top:0; }} }}
</style>
</head>
<body>
<main>
  <h1>User Story Test Report</h1>
  <p class="sub">CSE 360 Q&amp;A System &middot; tests run {when}</p>

  <div class="banner {'pass' if all_ok else 'fail'}">
    <div class="big">{passed}/{total}</div>
    <p><strong>{'All user story tests passed.' if all_ok else 'Some user story tests did not pass.'}</strong><br>
       Each test acts like a real user (logs in, then asks, answers, reviews or invites) and checks the app does the right thing.</p>
  </div>
{''.join(cards)}

  <footer>
    Generated from JUnit 5 results (<code>target/surefire-reports</code>) for
    <code>UserStoryAcceptanceTest.java</code>. To refresh: run <code>mvn clean test</code>,
    then <code>python3 docs/make-story-report.py</code>.
  </footer>
</main>
</body>
</html>
"""


if __name__ == "__main__":
    stories, sentences = read_display_names()
    results = read_results()
    if not results:
        raise SystemExit("No results found. Run 'mvn clean test' first.")
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(build_page(stories, sentences, results))
    passed = sum(1 for k in sentences if results.get(k) == "pass")
    print(f"Wrote {OUT}  ({passed}/{len(sentences)} passed)")
