import java.util.*;
/** Implement these algorithms. docs/guide/CONTRACTS.md defines each field and limit. */
public final class Algorithms {

    // ---------- helpers ----------
    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> in, String key) {
        Object v = in.get(key);
        return v == null ? new ArrayList<>() : (List<Object>) v;
    }

    private static List<Long> boxed(int[] a, int len) {
        List<Long> r = new ArrayList<>(len);
        for (int i = 0; i < len; i++) r.add((long) a[i]);
        return r;
    }

    private static Map<String, Object> positions(List<Long> pos) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("positions", pos);
        return r;
    }

    // пустой шаблон совпадает на каждой границе 0..n
    private static List<Long> allBoundaries(int n) {
        List<Long> r = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) r.add((long) i);
        return r;
    }

    /** Compare two versions of a sequence while preserving the order of shared characters. Implement lcs according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        int n = a.length(), m = b.length();
        // dp[i][j] = LCS of a[0..i) and b[0..j)
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        StringBuilder sb = new StringBuilder();
        int i = n, j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sb.append(a.charAt(i - 1));
                i--; j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("length", (long) dp[n][m]);
        r.put("sequence", sb.reverse().toString());
        return r;
    }

    /** Plan the cheapest sequence of adjacent block merges when the original order must be preserved. Implement merge according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> merge(Map<String, Object> input) {
        List<Object> raw = list(input, "sizes");
        int n = raw.size();
        Map<String, Object> r = new LinkedHashMap<>();
        List<List<Long>> plan = new ArrayList<>();
        if (n <= 1) {
            r.put("cost", 0L);
            r.put("plan", plan);
            return r;
        }
        long[] pref = new long[n + 1];
        for (int i = 0; i < n; i++) pref[i + 1] = pref[i] + ((Number) raw.get(i)).longValue();

        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int len = 2; len <= n; len++) {
            for (int l = 0; l + len - 1 < n; l++) {
                int rr = l + len - 1;
                long best = Long.MAX_VALUE;
                int bk = l;
                for (int k = l; k < rr; k++) {
                    long c = dp[l][k] + dp[k + 1][rr];
                    if (c < best) { best = c; bk = k; }
                }
                dp[l][rr] = best + pref[rr + 1] - pref[l];
                split[l][rr] = bk;
            }
        }

        // восстановление без рекурсии: сначала дети, потом родитель (postorder)
        Deque<int[]> st = new ArrayDeque<>();
        st.push(new int[]{0, n - 1, 0});
        while (!st.isEmpty()) {
            int[] top = st.peek();
            int l = top[0], rr = top[1];
            if (l == rr) { st.pop(); continue; }
            int k = split[l][rr];
            if (top[2] == 0) {
                top[2] = 1;
                st.push(new int[]{k + 1, rr, 0});
                st.push(new int[]{l, k, 0});
            } else {
                st.pop();
                plan.add(Arrays.asList((long) l, (long) k, (long) rr));
            }
        }
        r.put("cost", dp[0][n - 1]);
        r.put("plan", plan);
        return r;
    }

    /** Choose a maximum-value set when neighbouring vertices are incompatible and conflicts form a tree. Implement tree according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        Map<String, Object> r = new LinkedHashMap<>();
        if (n == 0) {
            r.put("weight", 0L);
            r.put("selected", new ArrayList<Long>());
            return r;
        }
        List<Object> wl = list(input, "weights");
        List<Object> el = list(input, "edges");
        long[] w = new long[n];
        for (int i = 0; i < n; i++) w[i] = ((Number) wl.get(i)).longValue();

        // список смежности в массивах (CSR), чтобы не было много объектов
        int[] deg = new int[n + 1];
        int[] eu = new int[el.size()], ev = new int[el.size()];
        for (int i = 0; i < el.size(); i++) {
            List<?> e = (List<?>) el.get(i);
            eu[i] = ((Number) e.get(0)).intValue();
            ev[i] = ((Number) e.get(1)).intValue();
            deg[eu[i]]++;
            deg[ev[i]]++;
        }
        int[] start = new int[n + 1];
        for (int i = 0; i < n; i++) start[i + 1] = start[i] + deg[i];
        int[] adj = new int[start[n]];
        int[] fill = Arrays.copyOf(start, n);
        for (int i = 0; i < eu.length; i++) {
            adj[fill[eu[i]]++] = ev[i];
            adj[fill[ev[i]]++] = eu[i];
        }

        // итеративный обход от корня 0 (рекурсия упадёт на длинной цепочке)
        int[] parent = new int[n];
        int[] order = new int[n];
        Arrays.fill(parent, -1);
        boolean[] seen = new boolean[n];
        int head = 0, tail = 0;
        order[tail++] = 0;
        seen[0] = true;
        while (head < tail) {
            int v = order[head++];
            for (int p = start[v]; p < start[v + 1]; p++) {
                int u = adj[p];
                if (!seen[u]) { seen[u] = true; parent[u] = v; order[tail++] = u; }
            }
        }

        long[] take = new long[n], skip = new long[n];
        for (int idx = n - 1; idx >= 0; idx--) {
            int v = order[idx];
            take[v] += w[v];
            int p = parent[v];
            if (p >= 0) {
                take[p] += skip[v];
                skip[p] += Math.max(take[v], skip[v]);
            }
        }

        boolean[] chosen = new boolean[n];
        List<Long> sel = new ArrayList<>();
        for (int idx = 0; idx < n; idx++) {
            int v = order[idx];
            int p = parent[v];
            boolean canTake = p < 0 || !chosen[p];
            if (canTake && take[v] > skip[v]) {
                chosen[v] = true;
                sel.add((long) v);
            }
        }
        r.put("weight", Math.max(take[0], skip[0]));
        r.put("selected", sel);
        return r;
    }

    /** Establish a simple exact-search baseline against which the other two searches can be checked. Implement naive according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> naive(Map<String, Object> input) {
        String t = (String) input.get("text");
        String p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        if (m == 0) return positions(allBoundaries(n));
        List<Long> res = new ArrayList<>();
        for (int i = 0; i + m <= n; i++) {
            int j = 0;
            while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
            if (j == m) res.add((long) i);
        }
        return positions(res);
    }

    static int[] prefixFunction(String p) {
        int m = p.length();
        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int k = pi[i - 1];
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        return pi;
    }

    /** Reuse information about matched prefixes to avoid restarting the search after every mismatch. Implement kmp according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> kmp(Map<String, Object> input) {
        String t = (String) input.get("text");
        String p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        if (m == 0) return positions(allBoundaries(n));
        List<Long> res = new ArrayList<>();
        if (m > n) return positions(res);
        int[] pi = prefixFunction(p);
        int k = 0;
        for (int i = 0; i < n; i++) {
            while (k > 0 && t.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (t.charAt(i) == p.charAt(k)) k++;
            if (k == m) {
                res.add((long) (i - m + 1));
                k = pi[k - 1]; // чтобы находить перекрытия
            }
        }
        return positions(res);
    }

    static final long BASE = 31, MOD = 101;

    /** Reject windows with unequal fingerprints and verify every equal-hash candidate. Implement rk according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> rk(Map<String, Object> input) {
        String t = (String) input.get("text");
        String p = (String) input.get("pattern");
        int n = t.length(), m = p.length();
        if (m == 0) return positions(allBoundaries(n));
        List<Long> res = new ArrayList<>();
        if (m > n) return positions(res);

        long high = 1; // BASE^(m-1) mod MOD
        for (int i = 0; i < m - 1; i++) high = high * BASE % MOD;
        long hp = 0, ht = 0;
        for (int i = 0; i < m; i++) {
            hp = (hp * BASE + p.charAt(i)) % MOD;
            ht = (ht * BASE + t.charAt(i)) % MOD;
        }
        for (int i = 0; ; i++) {
            if (ht == hp) {
                // одинаковый хеш ещё не значит совпадение, проверяем символы
                int j = 0;
                while (j < m && t.charAt(i + j) == p.charAt(j)) j++;
                if (j == m) res.add((long) i);
            }
            if (i + m >= n) break;
            ht = (ht - t.charAt(i) * high) % MOD;
            if (ht < 0) ht += MOD;
            ht = (ht * BASE + t.charAt(i + m)) % MOD;
        }
        return positions(res);
    }

    /** Order the text suffixes and use adjacent common prefixes to find repeated substrings. Implement suffix according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> suffix(Map<String, Object> input) {
        String s = (String) input.get("text");
        int n = s.length();
        Map<String, Object> r = new LinkedHashMap<>();
        if (n == 0) {
            r.put("sa", new ArrayList<Long>());
            r.put("lcp", new ArrayList<Long>());
            r.put("repeated", "");
            return r;
        }
        int[] sa = new int[n], rank = new int[n], tmp = new int[n], buf = new int[n];
        for (int i = 0; i < n; i++) { sa[i] = i; rank[i] = s.charAt(i); }
        int[] cnt = new int[Math.max(128, n) + 1];

        // prefix doubling, каждый раунд две сортировки подсчётом (radix по парам)
        for (int k = 1; ; k <<= 1) {
            int maxRank = Math.max(128, n);
            Arrays.fill(cnt, 0);
            for (int i = 0; i < n; i++) cnt[i + k < n ? rank[i + k] + 1 : 0]++;
            for (int i = 1; i <= maxRank; i++) cnt[i] += cnt[i - 1];
            for (int i = n - 1; i >= 0; i--) buf[--cnt[i + k < n ? rank[i + k] + 1 : 0]] = i;
            Arrays.fill(cnt, 0);
            for (int i = 0; i < n; i++) cnt[rank[i]]++;
            for (int i = 1; i <= maxRank; i++) cnt[i] += cnt[i - 1];
            for (int i = n - 1; i >= 0; i--) sa[--cnt[rank[buf[i]]]] = buf[i];

            tmp[sa[0]] = 0;
            for (int i = 1; i < n; i++) {
                int a = sa[i - 1], b = sa[i];
                int a2 = a + k < n ? rank[a + k] : -1;
                int b2 = b + k < n ? rank[b + k] : -1;
                tmp[b] = tmp[a] + ((rank[a] != rank[b] || a2 != b2) ? 1 : 0);
            }
            System.arraycopy(tmp, 0, rank, 0, n);
            if (rank[sa[n - 1]] == n - 1) break;
            if (k >= n) break;
        }

        // Kasai
        int[] lcp = new int[n];
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) pos[sa[i]] = i;
        int h = 0, best = 0, bestAt = 0;
        for (int i = 0; i < n; i++) {
            if (pos[i] == 0) { h = 0; continue; }
            int j = sa[pos[i] - 1];
            while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
            lcp[pos[i]] = h;
            if (h > best) { best = h; bestAt = i; }
            if (h > 0) h--;
        }
        r.put("sa", boxed(sa, n));
        r.put("lcp", boxed(lcp, n));
        r.put("repeated", s.substring(bestAt, bestAt + best));
        return r;
    }

    static final int ALPHA = 95; // ASCII 32..126

    /** Build one dictionary automaton and count its patterns in several independent texts. Implement aho according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> aho(Map<String, Object> input) {
        List<Object> pats = list(input, "patterns");
        List<Object> texts = list(input, "texts");
        int p = pats.size();
        long total = 1;
        for (Object o : pats) total += ((String) o).length();
        int maxStates = (int) total;

        int[][] go = new int[maxStates][];
        go[0] = new int[ALPHA];
        Arrays.fill(go[0], -1);
        int states = 1;
        int[] terminal = new int[p]; // дубли шаблонов делят одно состояние
        for (int i = 0; i < p; i++) {
            String s = (String) pats.get(i);
            int v = 0;
            for (int c = 0; c < s.length(); c++) {
                int ch = s.charAt(c) - 32;
                if (go[v][ch] == -1) {
                    go[states] = new int[ALPHA];
                    Arrays.fill(go[states], -1);
                    go[v][ch] = states++;
                }
                v = go[v][ch];
            }
            terminal[i] = v;
        }

        // BFS: суффиксные ссылки и полный автомат переходов
        int[] link = new int[states];
        int[] bfs = new int[states];
        int head = 0, tail = 0;
        for (int c = 0; c < ALPHA; c++) {
            int u = go[0][c];
            if (u == -1) go[0][c] = 0;
            else { link[u] = 0; bfs[tail++] = u; }
        }
        while (head < tail) {
            int v = bfs[head++];
            for (int c = 0; c < ALPHA; c++) {
                int u = go[v][c];
                if (u == -1) go[v][c] = go[link[v]][c];
                else { link[u] = go[link[v]][c]; bfs[tail++] = u; }
            }
        }

        List<List<Long>> counts = new ArrayList<>();
        long[] visits = new long[states];
        for (Object to : texts) {
            String t = (String) to;
            Arrays.fill(visits, 0);
            int v = 0;
            for (int i = 0; i < t.length(); i++) {
                v = go[v][t.charAt(i) - 32];
                visits[v]++;
            }
            // проталкиваем посещения вверх по суффиксным ссылкам (обратный порядок BFS)
            for (int i = tail - 1; i >= 0; i--) {
                int u = bfs[i];
                visits[link[u]] += visits[u];
            }
            List<Long> row = new ArrayList<>(p);
            for (int i = 0; i < p; i++) row.add(visits[terminal[i]]);
            counts.add(row);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("counts", counts);
        return r;
    }
}