package com.bootsignal.domain.work24.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.work24.config.Work24CrawlerProperties;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverview;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Work24CrawlerServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final Work24CrawlerService work24CrawlerService = new Work24CrawlerService(
		objectMapper,
		new Work24CrawlerProperties(
			"https://www.work24.go.kr/hr/a/a/3100/selectTracseDetl.do",
			"build/crawled/test.json",
			10_000,
			"test-agent"
		)
	);

	@TempDir
	Path tempDir;

	@Test
	void parseExtractsTrainingRequirementsAndGoal() {
		String html = """
			<div id="traCrseinfo">
				<table>
					<tbody>
						<tr>
							<th>훈련대상자요건</th>
							<td>[훈련대상자]<br>- 취업 준비생<br><br>[선수학습]<br>- 고등학교 이상 수준</td>
						</tr>
						<tr>
							<th>훈련목표</th>
							<td>[인재상]<br>- 데이터 처리 역량 확보<br><br>[프로젝트 역량]<br>- 프로젝트 수행 가능</td>
						</tr>
					</tbody>
				</table>
			</div>
			""";

		Work24TrainingCourseOverview overview = work24CrawlerService.parse(
			Jsoup.parse(html),
			"https://example.com/course",
			Instant.parse("2026-05-27T00:00:00Z")
		);

		assertThat(overview.trainingTargetRequirements())
			.isEqualTo("[훈련대상자]\n- 취업 준비생\n\n[선수학습]\n- 고등학교 이상 수준");
		assertThat(overview.trainingGoal())
			.isEqualTo("[인재상]\n- 데이터 처리 역량 확보\n\n[프로젝트 역량]\n- 프로젝트 수행 가능");
		assertThat(overview.sourceUrl()).isEqualTo("https://example.com/course");
		assertThat(overview.crawledAt()).isEqualTo(Instant.parse("2026-05-27T00:00:00Z"));
	}

	@Test
	void saveWritesOverviewAsJson() throws Exception {
		Work24TrainingCourseOverview overview = new Work24TrainingCourseOverview(
			"https://example.com/course",
			"[훈련대상자]\n- 취업 준비생",
			"[인재상]\n- 데이터 처리 역량 확보",
			Instant.parse("2026-05-27T00:00:00Z")
		);
		Path outputPath = tempDir.resolve("overview.json");

		Path savedPath = work24CrawlerService.save(overview, outputPath);

		assertThat(savedPath).isEqualTo(outputPath.toAbsolutePath().normalize());
		assertThat(Files.exists(savedPath)).isTrue();
		JsonNode jsonNode = objectMapper.readTree(savedPath.toFile());
		assertThat(jsonNode.get("trainingTargetRequirements").asText()).isEqualTo("[훈련대상자]\n- 취업 준비생");
		assertThat(jsonNode.get("trainingGoal").asText()).isEqualTo("[인재상]\n- 데이터 처리 역량 확보");
	}
}
