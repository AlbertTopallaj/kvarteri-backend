package se.kvarteri.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks brute-force attempts on HTTP Basic: after {@code maxFailures} 401s for the same
 * client ip + username within the window the client gets 429 until the lockout ends.
 * In-memory, so per instance; use a shared store if the backend is scaled out.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {
    }

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration window;
    private final Duration lockout;
    private final Clock clock;

    public LoginThrottleFilter(int maxFailures, Duration window, Duration lockout, Clock clock) {
        this.maxFailures = maxFailures;
        this.window = window;
        this.lockout = lockout;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String username = basicUsername(req.getHeader("Authorization"));
        if (username == null) {
            chain.doFilter(req, res);
            return;
        }
        // remoteAddr only: X-Forwarded-For is client controlled (configure forwarded headers on the proxy).
        String key = req.getRemoteAddr() + "|" + username;
        Instant now = clock.instant();
        Attempts a = attempts.get(key);
        if (a != null && a.lockedUntil() != null && now.isBefore(a.lockedUntil())) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(Duration.between(now, a.lockedUntil()).toSeconds() + 1));
            return;
        }
        chain.doFilter(req, res);
        if (res.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            recordFailure(key, now);
        } else {
            attempts.remove(key);
        }
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(e -> expired(e.getValue(), now));
        }
    }

    private void recordFailure(String key, Instant now) {
        attempts.compute(key, (k, old) -> {
            if (old == null || expired(old, now)) {
                return new Attempts(1, now, null);
            }
            int f = old.failures() + 1;
            return f >= maxFailures ? new Attempts(0, now, now.plus(lockout)) : new Attempts(f, old.windowStart(), null);
        });
    }

    private boolean expired(Attempts a, Instant now) {
        if (a.lockedUntil() != null) return now.isAfter(a.lockedUntil());
        return now.isAfter(a.windowStart().plus(window));
    }

    private static String basicUsername(String header) {
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()),
                    StandardCharsets.UTF_8);
            int i = decoded.indexOf(':');
            return (i < 0 ? decoded : decoded.substring(0, i)).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
