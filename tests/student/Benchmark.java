import java.util.*;

// сравнение naive / kmp / rk на одинаковых входах
public final class Benchmark {
    static final int RUNS = 5, REPS = 50, WARMUP = 200;

    static String randomText(Random rnd, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append((char) ('a' + rnd.nextInt(4)));
        return sb.toString();
    }

    static Map<String, Object> req(String text, String pattern) {
        Map<String, Object> m = new HashMap<>();
        m.put("text", text);
        m.put("pattern", pattern);
        return m;
    }

    interface Search { Map<String, Object> run(Map<String, Object> in); }

    public static void main(String[] args) {
        long seed = 42;
        Random rnd = new Random(seed);
        int[] sizes = {1000, 2000, 3000};
        String[] names = {"naive", "kmp", "rk"};
        Search[] algs = {Algorithms::naive, Algorithms::kmp, Algorithms::rk};

        System.out.println("|Case|n|m|Seed/generator|Algorithm|Run 1|Run 2|Run 3|Run 4|Run 5|Median|Correct outputs checked|");
        System.out.println("|---|---:|---:|---|---|---:|---:|---:|---:|---:|---:|---|");
        for (String kind : new String[]{"random", "repetitive"}) {
            for (int n : sizes) {
                int m = n / 10;
                String text, pat;
                if (kind.equals("random")) {
                    text = randomText(rnd, n);
                    int st = rnd.nextInt(n - m);
                    pat = text.substring(st, st + m); // шаблон из текста, чтобы было хоть одно совпадение
                } else {
                    text = "a".repeat(n);
                    pat = "a".repeat(m - 1) + "b"; // худший случай для naive
                }
                Map<String, Object> in = req(text, pat);
                Object expected = algs[0].run(in).get("positions");
                boolean same = true;
                for (Search s : algs) same &= s.run(in).get("positions").equals(expected);

                for (int a = 0; a < algs.length; a++) {
                    for (int w = 0; w < WARMUP; w++) algs[a].run(in); // прогрев JIT
                    double[] t = new double[RUNS];
                    for (int r = 0; r < RUNS; r++) {
                        long t0 = System.nanoTime();
                        for (int k = 0; k < REPS; k++) algs[a].run(in);
                        t[r] = (System.nanoTime() - t0) / 1e6 / REPS;
                    }
                    double[] sorted = t.clone();
                    Arrays.sort(sorted);
                    StringBuilder row = new StringBuilder();
                    row.append("|").append(kind).append("|").append(n).append("|").append(m)
                            .append("|java.util.Random(").append(seed).append(")|").append(names[a]).append("|");
                    for (double x : t) row.append(String.format(Locale.US, "%.4f", x)).append("|");
                    row.append(String.format(Locale.US, "%.4f", sorted[RUNS / 2])).append("|")
                            .append(same ? "yes" : "NO").append("|");
                    System.out.println(row);
                }
            }
        }
    }
}
