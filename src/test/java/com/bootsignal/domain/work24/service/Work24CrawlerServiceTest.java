package com.bootsignal.domain.work24.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bootsignal.domain.work24.config.Work24CrawlerProperties;
import com.bootsignal.domain.work24.dto.Work24TrainingCourseOverview;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
	void parseExtractsTrainingCourseAndInstitutionDetails() {
		String courseHtml = """
			<div class="etc_info">
				<div class="flex-wrap">
					<span class="count">수강확정인원 <strong class="clr_orange">18명</strong></span> /
					<span class="count ml0">선발인원 <strong class="clr_blue">18명</strong></span> /
					<span class="count ml0">모집인원 <strong>60명</strong></span>
				</div>
			</div>
			<script>
				newEmpymnRt = '37.8';
			</script>
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
		String institutionHtml = """
			<div class="box_btn_group">
				<img src="/hr/z/z/0000/hrdFileDownLoad.do?athfilId=test&athfilSeqNo=4" alt="훈련기관사진">
			</div>
			<div class="box_group_wrap">
				<div class="title">
					<h3 class="t2_sb h3_flex_type">훈련기관 소개</h3>
				</div>
			</div>
			<div class="addExplainBoxArea">
				<div class="my_info_area">
					<pre class="txt">그렙 소개
			교육, 평가, 채용 서비스 제공</pre>
				</div>
			</div>
			""";

		Work24TrainingCourseOverview overview = work24CrawlerService.parse(
			Jsoup.parse(courseHtml),
			Jsoup.parse(institutionHtml, "https://www.work24.go.kr/hr/a/a/3200/selectTrainInstitutionPost.do"),
			"https://example.com/course",
			Instant.parse("2026-05-27T00:00:00Z")
		);

		assertThat(overview.trainingTargetRequirements())
			.isEqualTo("[훈련대상자]\n- 취업 준비생\n\n[선수학습]\n- 고등학교 이상 수준");
		assertThat(overview.trainingGoal())
			.isEqualTo("[인재상]\n- 데이터 처리 역량 확보\n\n[프로젝트 역량]\n- 프로젝트 수행 가능");
		assertThat(overview.confirmedTraineeCount()).isEqualTo(18);
		assertThat(overview.selectedTraineeCount()).isEqualTo(18);
		assertThat(overview.recruitmentCount()).isEqualTo(60);
		assertThat(overview.employmentRate()).isEqualByComparingTo(new BigDecimal("37.8"));
		assertThat(overview.institutionProfileImageUrl())
			.isEqualTo("https://www.work24.go.kr/hr/z/z/0000/hrdFileDownLoad.do?athfilId=test&athfilSeqNo=4");
		assertThat(overview.institutionIntroduction()).isEqualTo("그렙 소개\n교육, 평가, 채용 서비스 제공");
		assertThat(overview.sourceUrl()).isEqualTo("https://example.com/course");
		assertThat(overview.crawledAt()).isEqualTo(Instant.parse("2026-05-27T00:00:00Z"));
	}

	@Test
	void saveWritesOverviewAsJson() throws Exception {
		Work24TrainingCourseOverview overview = new Work24TrainingCourseOverview(
			"https://example.com/course",
			"[훈련대상자]\n- 취업 준비생",
			"[인재상]\n- 데이터 처리 역량 확보",
			18,
			18,
			60,
			new BigDecimal("37.8"),
			"https://www.work24.go.kr/hr/z/z/0000/hrdFileDownLoad.do?athfilId=test&athfilSeqNo=4",
			"그렙 소개",
			Instant.parse("2026-05-27T00:00:00Z")
		);
		Path outputPath = tempDir.resolve("overview.json");

		Path savedPath = work24CrawlerService.save(overview, outputPath);

		assertThat(savedPath).isEqualTo(outputPath.toAbsolutePath().normalize());
		assertThat(Files.exists(savedPath)).isTrue();
		JsonNode jsonNode = objectMapper.readTree(savedPath.toFile());
		assertThat(jsonNode.get("trainingTargetRequirements").asText()).isEqualTo("[훈련대상자]\n- 취업 준비생");
		assertThat(jsonNode.get("trainingGoal").asText()).isEqualTo("[인재상]\n- 데이터 처리 역량 확보");
		assertThat(jsonNode.get("confirmedTraineeCount").asInt()).isEqualTo(18);
		assertThat(jsonNode.get("selectedTraineeCount").asInt()).isEqualTo(18);
		assertThat(jsonNode.get("recruitmentCount").asInt()).isEqualTo(60);
		assertThat(jsonNode.get("employmentRate").decimalValue()).isEqualByComparingTo(new BigDecimal("37.8"));
		assertThat(jsonNode.get("institutionProfileImageUrl").asText())
			.isEqualTo("https://www.work24.go.kr/hr/z/z/0000/hrdFileDownLoad.do?athfilId=test&athfilSeqNo=4");
		assertThat(jsonNode.get("institutionIntroduction").asText()).isEqualTo("그렙 소개");
	}
}
