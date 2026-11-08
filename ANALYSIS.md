# Assignment 4 — analysis

## Environment and commands

- Language: Java. JDK 25.0.1 (Oracle HotSpot), compiled with `--release 17`.
- Launcher: Python 3.12.0.
- OS: Windows 11 Pro. CPU: Intel Core 5 210H. RAM: 16 GB.

Commands (from repository root):

```
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/student/my_tests.jsonl
javac --release 17 -encoding UTF-8 -d bench Java/source/*.java tests/student/Benchmark.java
java -cp bench Benchmark
```

`--check` result: all 44 public cases PASS, and the repeated mixed batch (state reset) PASS.

## Algorithm explanations

### lcs

- **State:** `dp[i][j]` = length of LCS of prefixes `a[0..i)` and `b[0..j)`.
- **Transition:** if `a[i-1] == b[j-1]` then `dp[i][j] = dp[i-1][j-1] + 1`, else `dp[i][j] = max(dp[i-1][j], dp[i][j-1])`.
- **Base:** `dp[0][j] = dp[i][0] = 0` (empty prefix). This also covers empty input strings.
- **Order:** rows i = 1..n, columns j = 1..m, so the top, left and diagonal cells are always ready.
- **Correctness:** if the last characters are equal, some optimal LCS uses them both, so we can match them. If they differ, at least one of them is not the last character of the LCS, so dropping one of them loses nothing, and we take the better of the two options.
- **Reconstruction:** walk back from `(n, m)`. On a match take the character and go diagonally, otherwise move to the neighbour with the bigger value (ties go up). Every step keeps `dp` on an optimal path, so the length is `dp[n][m]`. The string is built backwards and then reversed. Ties can give a different but still optimal sequence.
- **Complexity:** O((n+1)(m+1)) time and memory for the int table. Output O(length).
- **Test:** `a="", b="abc"` gives length 0, `""`. This catches a missing empty-string case.

### merge

- **State:** `dp[l][r]` = minimum cost to merge original blocks `l..r` into one block. `split[l][r]` = the best last split `k`.
- **Transition:** `dp[l][r] = min over k in [l, r-1] of dp[l][k] + dp[k+1][r] + sum(l..r)`. The last merge always joins `[l..k]` and `[k+1..r]` and costs the whole interval sum, whatever the order before it. The sum comes from prefix sums `pref` in O(1).
- **Base:** `dp[i][i] = 0`. For n ≤ 1 we return cost 0 and an empty plan.
- **Order:** by increasing interval length, so shorter subintervals are computed first.
- **Correctness:** every valid merge sequence of `[l..r]` ends with one final merge at some split k, and the two sides are independent subproblems (optimal substructure). We try all k.
- **Reconstruction:** an iterative postorder over the split tree (an explicit stack, no recursion). A merge `[l,k,r]` is printed only after both children are fully merged, so the plan is chronological and each step joins two blocks that already exist. It has exactly n-1 merges.
- **Complexity:** O(n³) time, O(n²) memory (`long` dp + `int` split). `long` is needed because costs can reach about 200·200·10⁹.
- **Test:** `[10⁹, 10⁹, 10⁹]` gives cost 5·10⁹. This would overflow with `int`.

### tree

- **State:** `take[v]` = best weight in v's subtree when v is selected. `skip[v]` = best weight when v is not selected.
- **Transition:** `take[v] = w[v] + sum skip[c]`, `skip[v] = sum max(take[c], skip[c])` over the children c.
- **Base:** a leaf has `take = w`, `skip = 0`. n = 0 returns weight 0 and `[]`.
- **Order:** BFS from root 0 gives an order where parents come before children. Processing it in reverse is a postorder, so the children are done before the parent. Everything is iterative because the input can be a chain of 100000 vertices, where recursion would overflow the stack.
- **Correctness:** if v is taken, no child can be taken. If v is skipped, every child is free. The answer is `max(take[root], skip[root])`. The empty set is allowed through `skip`, so all-negative weights give 0. The root choice does not matter, because both states cover every possibility.
- **Reconstruction:** go top-down in BFS order. A vertex is selected only if its parent is not selected and `take[v] > skip[v]`. When the parent is taken, the child is forced to skip, which matches how `take[parent]` was computed. So no two adjacent vertices are selected and the sum equals the optimum. On a tie we skip.
- **Complexity:** O(n) time and memory. Adjacency is stored as CSR arrays.
- **Test:** all-negative weights `[-5,-1,-7]` give weight 0 and `[]`. This catches code that is forced to pick at least one vertex.

### naive

- **Invariant:** we check every start `i` from 0 to n-m (the last valid start is n-m) and compare characters until the first mismatch.
- Overlaps are found automatically, because each start is checked on its own. An empty pattern returns all boundaries `0..n` (n+1 positions). If m > n the loop never runs and the result is `[]`.
- **Complexity:** O(n·m + n + 1) time, O(1) auxiliary memory, O(z) output.
- **Test:** `text="abc", pattern=""` gives `[0,1,2,3]`, including the last boundary 3.

### kmp

- **pi[i]:** the length of the longest proper prefix of `pattern[0..i]` that is also its suffix.
- **Building pi:** start from `k = pi[i-1]` and fall back with `k = pi[k-1]` while the characters differ, then extend by one on a match.
- **Search:** `k` is the number of pattern characters matched so far. On a mismatch, fall back with `k = pi[k-1]`, so we never move backwards in the text. After a full match (`k == m`), report `i-m+1` and set `k = pi[m-1]`, so overlapping matches are still found.
- An empty pattern is handled before any indexing. m > n returns `[]`.
- **Complexity:** O(n + m + z) time, because k grows at most n times in total, so the fallbacks are bounded too. O(m) auxiliary memory for pi.
- **Test:** `text="aaaa", pattern="aa"` gives `[0,1,2]`. Resetting k to 0 after a match would give `[0,2]`.

### rk

- **Hash:** `h(s) = (s[0]·31^(m-1) + ... + s[m-1]) mod 101` with ASCII codes, so the leftmost character has the highest power.
- **Rolling update:** `h = ((h - t[i]·31^(m-1)) mod 101 + 101) mod 101`, then `h = (h·31 + t[i+m]) mod 101`. The `+101` fixes negative remainders in Java.
- **Verification:** an equal hash only marks a candidate window. We compare all m characters before adding the position. With mod 101 collisions are frequent, so this check is required.
- m = 0 and m > n are handled before the first window is built.
- **Complexity:** O(n + m + c·m), where c is the number of equal-hash candidates. In the worst case (many collisions or many real matches) this is O(n·m). It is not guaranteed linear. O(1) auxiliary memory besides the output.
- **Test:** `"abdj"` with pattern `"dj"` gives `[2]`, and with `"ab"` gives `[0]`. `ab` and `dj` both hash to 75, so without verification both positions would be reported.

### suffix

**Prefix doubling.** `rank[i]` is the class of the first k characters of suffix i (initially the ASCII code). In each round the suffixes are sorted by the pair `(rank[i], rank[i+k])`, where a missing second half counts as smallest. Then new ranks are given out, and equal pairs share a rank. After the round, ranks describe the first 2k characters. I used two stable counting sorts per round (second key, then first key), which is a radix sort of the pairs, so each round is O(n) and the total is O(n log n). It stops when all ranks are different. No sentinel is added and no suffix strings are created.

**Kasai.** `pos[sa[i]] = i`. Go through suffixes in text order i = 0..n-1 and compare suffix i with the suffix just before it in `sa`. The key fact is that if `lcp` for suffix i is h, then for suffix i+1 it is at least h-1. So h only decreases by 1 per step, and the total work is O(n). `lcp[0] = 0`.

**Longest repeat.** Any repeated substring is a common prefix of two suffixes. The longest common prefix of two suffixes equals the minimum of the lcp values between them in sorted order, so the maximum is reached by some adjacent pair. That means `max(lcp)` is the length of the longest repeat, and it starts at that suffix. Overlapping occurrences are allowed. If `max(lcp) = 0` we return `""`.

- **Complexity:** O(n log n) time, O(n) memory. Empty text gives `[]`, `[]`, `""`.
- **Test:** `"aaaa"` gives `sa=[3,2,1,0]`, `lcp=[0,1,2,3]`, repeated `"aaa"` (overlapping). `"abc"` gives repeated `""`.

### aho

- **Trie:** one node per distinct prefix, with an array of 95 transitions (ASCII 32..126). `terminal[i]` is the node where pattern i ends. Duplicate patterns end at the same node, so they automatically get equal counts.
- **Failure links:** built by BFS. `link[u]` is the longest proper suffix of u's string that is also a trie node. Missing transitions are filled in as `go[v][c] = go[link[v]][c]`, which makes a full automaton, so the search never has to loop.
- **Counting:** for each text, reset `visits` to 0, run the automaton and do `visits[state]++` for each character. Then go through the nodes in reverse BFS order and add `visits[u]` into `visits[link[u]]`. Now `visits[x]` is the number of positions where the string of x ends, which also covers suffix patterns (for example "he" inside "she") and overlaps. The answer for pattern i is `visits[terminal[i]]`.
- **Independent texts:** the counts are reset for every text, and the automaton is rebuilt for every request. An empty dictionary gives `[]` per text, empty texts give `[]`, and an empty text gives a row of zeros.
- **Complexity:** construction O(L + 95·S + p), per text O(|text| + S + p), memory O(95·S + p). `long` counts.
- **Test:** patterns `["a","a","aa"]` and text `"aaa"` give `[3,3,2]`. Duplicates are equal and overlapping `"aa"` is counted twice.

## Own tests

File: `tests/student/my_tests.jsonl`, run with `python tools/run.py --input tests/student/my_tests.jsonl`.

|Command|Input|Expected result|Actual result|What it checks|
|---|---|---|---|---|
|lcs|`a="", b="abc"`|length 0, `""`|`{"length":0,"sequence":""}`|empty string|
|lcs|`a="aaaa", b="aa"`|length 2, `"aa"`|`{"length":2,"sequence":"aa"}`|repeated characters|
|merge|`[0,0,0]`|cost 0, 2 merges|`{"cost":0,"plan":[[1,1,2],[0,0,2]]}`|zeros, plan still has n-1 merges|
|merge|`[1e9,1e9,1e9]`|cost 5000000000|`{"cost":5000000000,"plan":[[1,1,2],[0,0,2]]}`|64-bit overflow|
|tree|n=3, weights `[-5,-1,-7]`, chain|weight 0, `[]`|`{"weight":0,"selected":[]}`|all negative, empty set|
|tree|n=1, weight `[5]`|weight 5, `[0]`|`{"weight":5,"selected":[0]}`|single vertex, no edges|
|naive|`"abc"`, pattern `""`|`[0,1,2,3]`|`{"positions":[0,1,2,3]}`|empty pattern, n+1 boundaries|
|kmp|`"aaaa"`, `"aa"`|`[0,1,2]`|`{"positions":[0,1,2]}`|overlaps after full match|
|rk|`"abdj"`, `"dj"`|`[2]`|`{"positions":[2]}`|collision with "ab" (hash 75) is rejected|
|suffix|`"aaaa"`|sa `[3,2,1,0]`, lcp `[0,1,2,3]`, `"aaa"`|`{"sa":[3,2,1,0],"lcp":[0,1,2,3],"repeated":"aaa"}`|all equal chars, overlapping repeat|
|suffix|`"abc"`|sa `[0,1,2]`, lcp `[0,0,0]`, `""`|`{"sa":[0,1,2],"lcp":[0,0,0],"repeated":""}`|no repeat|
|aho|`["a","a","aa"]`, texts `["aaa",""]`|`[[3,3,2],[0,0,0]]`|`{"counts":[[3,3,2],[0,0,0]]}`|duplicates, overlaps, empty text|

The public checks also run the full batch twice in one process (consecutive requests / state reset), and that passed.

## Search comparison

Generator: `tests/student/Benchmark.java`, `java.util.Random(42)`.

- **random:** a text over the alphabet {a,b,c,d}. The pattern is a substring of the text taken at a random position (m = n/10).
- **repetitive:** text `a^n`, pattern `a^(m-1)b` with m = n/10. This is the worst case for naive (m comparisons at almost every start) and has no matches.

Before timing, the program checks that naive, kmp and rk return the same `positions` list (last column). The timing scope is only the algorithm call (the `Algorithms.xxx(map)` call, without JSON parsing or printing), using `System.nanoTime()`. For JIT, there are 200 warm-up calls per algorithm before measuring. Each run is 50 calls, and the time shown is ms per call. There are 5 runs, and the median is the middle value. Compilation is not included. All runs were on the machine described above, with the same JVM settings for all methods.

|Case|n|m|Seed/generator|Algorithm|Run 1|Run 2|Run 3|Run 4|Run 5|Median|Correct outputs checked|
|---|---:|---:|---|---|---:|---:|---:|---:|---:|---:|---|
|random|1000|100|java.util.Random(42)|naive|0.0157|0.0204|0.0163|0.0203|0.0228|0.0203|yes|
|random|1000|100|java.util.Random(42)|kmp|0.0148|0.0157|0.0147|0.0155|0.0147|0.0148|yes|
|random|1000|100|java.util.Random(42)|rk|0.0134|0.0137|0.0135|0.0133|0.0131|0.0134|yes|
|random|2000|200|java.util.Random(42)|naive|0.0034|0.0034|0.0033|0.0033|0.0034|0.0034|yes|
|random|2000|200|java.util.Random(42)|kmp|0.0351|0.0371|0.0351|0.0353|0.0354|0.0353|yes|
|random|2000|200|java.util.Random(42)|rk|0.0426|0.0381|0.0374|0.0454|0.0451|0.0426|yes|
|random|3000|300|java.util.Random(42)|naive|0.0054|0.0078|0.0047|0.0065|0.0079|0.0065|yes|
|random|3000|300|java.util.Random(42)|kmp|0.0068|0.0066|0.0067|0.0066|0.0068|0.0067|yes|
|random|3000|300|java.util.Random(42)|rk|0.0189|0.0194|0.0194|0.0194|0.0199|0.0194|yes|
|repetitive|1000|100|java.util.Random(42)|naive|0.0967|0.0960|0.0957|0.0955|0.1030|0.0960|yes|
|repetitive|1000|100|java.util.Random(42)|kmp|0.0038|0.0039|0.0038|0.0040|0.0038|0.0038|yes|
|repetitive|1000|100|java.util.Random(42)|rk|0.0065|0.0065|0.0069|0.0063|0.0067|0.0065|yes|
|repetitive|2000|200|java.util.Random(42)|naive|0.3750|0.3738|0.3758|0.3777|0.3927|0.3758|yes|
|repetitive|2000|200|java.util.Random(42)|kmp|0.0072|0.0072|0.0074|0.0073|0.0072|0.0072|yes|
|repetitive|2000|200|java.util.Random(42)|rk|0.0122|0.0122|0.0126|0.0133|0.0126|0.0126|yes|
|repetitive|3000|300|java.util.Random(42)|naive|0.8453|0.8614|0.8443|0.8406|0.8269|0.8443|yes|
|repetitive|3000|300|java.util.Random(42)|kmp|0.0109|0.0113|0.0108|0.0108|0.0108|0.0108|yes|
|repetitive|3000|300|java.util.Random(42)|rk|0.0181|0.0202|0.0296|0.0301|0.0249|0.0249|yes|

**Observations.**

- On repetitive input, naive is clearly the slowest and grows roughly 4x when n doubles (0.096 → 0.376 → 0.844 ms). That matches about n·m comparisons, because m grows with n here. KMP and RK grow roughly linearly.
- On random input, naive is actually very fast, because with a 4-letter alphabet most windows fail at the first or second character. So its real cost is close to O(n). The random-case numbers are very small (microseconds) and noisy. The n=1000 row was measured first and is probably still affected by JIT warm-up and CPU frequency, so I would not draw conclusions from it.
- RK is a bit slower than KMP here. It has to do modular arithmetic for every window, and with mod 101 about 1 in 101 windows is a false candidate that needs a full m-character check. In the repetitive case the text window `a^m` and the pattern `a^(m-1)b` have different hashes, so RK did not fall into its quadratic case. But with mod 101 it is easy to construct inputs where many windows collide, and then RK becomes O(n·m) like naive.
- The modulus 101 is chosen on purpose to make collisions testable (the `ab`/`dj` test). Real implementations use a large modulus (around 10⁹ or 2⁶¹-1), sometimes with double hashing or a random base, so collisions are much rarer. These results do not describe those implementations.
- These are timing medians on one machine. They are consistent with the expected complexities but do not prove them.

## Sources and AI assistance

- Course materials: assignment specification, `docs/guide/*`, lecture notes for weeks 8–10, `LECTURE_CLARIFICATIONS_EN_RU.md`.
- AI: the benchmark `tests/student/Benchmark.java` and to explain the tasks
- My own verification: I ran `--setup` and `--check` (all public cases pass), ran my own tests from `tests/student/my_tests.jsonl` and checked the outputs by hand against the expected results in the table, and ran the benchmark on my computer. I read through the code and the explanations above.
