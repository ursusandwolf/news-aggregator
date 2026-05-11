package com.moneynews.integration.sec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneynews.domain.model.Filing;
import com.moneynews.domain.port.FilingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EdgarFilingProvider implements FilingProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    // SEC requires a specific User-Agent format: CompanyName AdminContact@email.com
    private static final String USER_AGENT = "MoneyNewsAggregator alex@example.com";

    public EdgarFilingProvider(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Filing> fetchFilings(String ticker, int limit) {
        // Step 1: Map ticker to CIK (Central Index Key)
        // For simplicity in this version, we'll use a mocked CIK mapping logic
        // In a production scenario, you'd fetch the ticker-cik.json from SEC
        String cik = getCikForTicker(ticker);
        if (cik == null) return Collections.emptyList();

        // Step 2: Query SEC Submissions API
        // Format: https://data.sec.gov/submissions/CIK##########.json
        String url = String.format("https://data.sec.gov/submissions/CIK%s.json", cik);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch filings for {} (CIK {}): HTTP {}", ticker, cik, response.statusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode recentFilings = root.path("filings").path("recent");
            
            JsonNode forms = recentFilings.path("form");
            JsonNode accessionNumbers = recentFilings.path("accessionNumber");
            JsonNode filingDates = recentFilings.path("filingDate");
            JsonNode primaryDocuments = recentFilings.path("primaryDocument");

            List<Filing> filings = new ArrayList<>();
            for (int i = 0; i < Math.min(forms.size(), limit); i++) {
                String form = forms.get(i).asText();
                // Filter for relevant forms only
                if (form.equals("10-K") || form.equals("10-Q") || form.equals("8-K")) {
                    String accession = accessionNumbers.get(i).asText();
                    String cleanAccession = accession.replace("-", "");
                    String primaryDoc = primaryDocuments.get(i).asText();
                    
                    // URL construction: https://www.sec.gov/Archives/edgar/data/{cik}/{accession}/{primaryDoc}
                    String reportUrl = String.format("https://www.sec.gov/Archives/edgar/data/%s/%s/%s", 
                        cik.replaceFirst("^0+(?!$)", ""), cleanAccession, primaryDoc);

                    filings.add(new Filing(
                        null,
                        ticker,
                        accession,
                        form,
                        filingDates.get(i).asText(),
                        reportUrl,
                        String.format("%s filing for %s", form, ticker)
                    ));
                }
            }
            return filings;

        } catch (Exception e) {
            log.error("Error fetching filings for {}: {}", ticker, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getCikForTicker(String ticker) {
        // SEC CIKs are 10-digit zero-padded strings.
        // Mocking common tickers for the prototype:
        return switch (ticker.toUpperCase()) {
            case "AAPL" -> "0000320193";
            case "MSFT" -> "0000789019";
            case "GOOGL" -> "0001652044";
            case "TSLA" -> "0001318605";
            case "NVDA" -> "0001045810";
            default -> null; // In reality, fetch from ticker-cik.json
        };
    }
}
