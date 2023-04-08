package suzumiya.filter;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component(value = "statisticsFilter")
@Slf4j
public class StatisticsFilter extends OncePerRequestFilter {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd");
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String today = LocalDate.now().format(formatter);

        String ip = request.getHeader("X-Real-IP");
        redisTemplate.opsForHyperLogLog().add("uv:" + today, ip); // uv
        redisTemplate.opsForHyperLogLog().add("pv:" + today, ip + "_" + System.currentTimeMillis()); // pv

        filterChain.doFilter(request, response);
    }
}
