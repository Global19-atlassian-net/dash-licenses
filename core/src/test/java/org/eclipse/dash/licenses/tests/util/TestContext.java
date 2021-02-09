package org.eclipse.dash.licenses.tests.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.dash.licenses.ISettings;
import org.eclipse.dash.licenses.context.DefaultContext;
import org.eclipse.dash.licenses.http.IHttpClientService;
import org.eclipse.dash.licenses.review.GitLabSupport;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;

/**
 * This class provides a context for testing that stubs out any behaviour that
 * might call out to an external service. Keep the tests local.
 */
public class TestContext extends DefaultContext {

	public TestContext() {
		super(new ISettings() {
		});
	}

	@Override
	public GitLabSupport getGitLabService() {
		return null;
	}

	@Override
	public IHttpClientService getHttpClientService() {
		return new IHttpClientService() {
			@Override
			public int post(String url, String contentType, String payload, Consumer<String> handler) {
				if (url.equals(TestContext.this.getSettings().getClearlyDefinedDefinitionsUrl())) {
					// The file contains only the information for the one record; the
					// ClearlyDefined service expects a Json collection as the response,
					// so insert the file contents into an array and pass that value to
					// the handler.
					JsonReader reader = Json.createReader(new StringReader(payload));
					JsonArray items = (JsonArray) reader.read();

					var builder = new StringBuilder();
					builder.append("{");
					for (int index = 0; index < items.size(); index++) {
						if (index > 0)
							builder.append(",");
						var id = items.getString(index);
						builder.append("\"");
						builder.append(id);
						builder.append("\" :");

						switch (id) {
						case "npm/npmjs/-/write/0.2.0":
							builder.append(new BufferedReader(new InputStreamReader(
									this.getClass().getResourceAsStream("/write-1.0.3.json"), StandardCharsets.UTF_8))
											.lines().collect(Collectors.joining("\n")));
							break;
						case "npm/npmjs/@yarnpkg/lockfile/1.1.0":
							builder.append(new BufferedReader(
									new InputStreamReader(this.getClass().getResourceAsStream("/lockfile-1.1.0.json"),
											StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n")));
							break;

						default:
							builder.append("{}");
						}
					}
					builder.append("}");

					handler.accept(builder.toString());

					return 200;
				}

				if (url.equals(TestContext.this.getSettings().getLicenseCheckUrl())) {
					handler.accept("{}");
					return 200;
				}
				return 404;
			}

			@Override
			public int get(String url, String contentType, Consumer<InputStream> handler) {
				if (url.equals(TestContext.this.getSettings().getApprovedLicensesUrl())) {
					handler.accept(this.getClass().getResourceAsStream("/licenses.json"));
					return 200;
				}

				return 404;
			}
		};
	}
}