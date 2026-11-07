import java.util.*;
/** Implement these algorithms. docs/guide/CONTRACTS.md defines each field and limit. */
public final class Algorithms {
    /** Compare two versions of a sequence while preserving the order of shared characters. Implement lcs according to docs/guide/CONTRACTS.md. */
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

    // пустой шаблон совпадает
    private static List<Long> allBoundaries(int n) {
        List<Long> r = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) r.add((long) i);
        return r;
    static Map<String,Object> lcs(Map<String,Object> input) {
        throw new UnsupportedOperationException("TODO lcs");
    }
    /** Plan the cheapest sequence of adjacent block merges when the original order must be preserved. Implement merge according to docs/guide/CONTRACTS.md. */
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
    /** Choose a maximum-value set when neighbouring vertices are incompatible and conflicts form a tree. Implement tree according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> tree(Map<String,Object> input) { throw new UnsupportedOperationException("TODO tree"); }
    /** Establish a simple exact-search baseline against which the other two searches can be checked. Implement naive according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> naive(Map<String,Object> input) { throw new UnsupportedOperationException("TODO naive"); }
    /** Reuse information about matched prefixes to avoid restarting the search after every mismatch. Implement kmp according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> kmp(Map<String,Object> input) { throw new UnsupportedOperationException("TODO kmp"); }
    /** Reject windows with unequal fingerprints and verify every equal-hash candidate. Implement rk according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> rk(Map<String,Object> input) { throw new UnsupportedOperationException("TODO rk"); }
    /** Order the text suffixes and use adjacent common prefixes to find repeated substrings. Implement suffix according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> suffix(Map<String,Object> input) { throw new UnsupportedOperationException("TODO suffix"); }
    /** Build one dictionary automaton and count its patterns in several independent texts. Implement aho according to docs/guide/CONTRACTS.md. */
    static Map<String,Object> aho(Map<String,Object> input) { throw new UnsupportedOperationException("TODO aho"); }
}
