/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.authorisation;

import com.google.common.base.Optional;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import util.user.UserRule;
import util.user.Users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

/**
 * TODO : Add missing ITs
 * - creating new user using email already used
 * - display multiple identity provider plugins (probably in another class)
 */
public class BaseIdentityProviderTest {

  @ClassRule
  public static Orchestrator ORCHESTRATOR = Category1Suite.ORCHESTRATOR;

  @Rule
  public UserRule userRule = UserRule.from(ORCHESTRATOR);

  static String FAKE_PROVIDER_KEY = "fake-base-id-provider";

  static String USER_LOGIN = "john";
  static String USER_PROVIDER_ID = "fake-john";
  static String USER_NAME = "John";
  static String USER_EMAIL = "john@email.com";

  static String USER_NAME_UPDATED = "John Doe";
  static String USER_EMAIL_UPDATED = "john.doe@email.com";

  static WsClient adminWsClient;

  @BeforeClass
  public static void setUp() {
    ORCHESTRATOR.resetData();
    adminWsClient = newAdminWsClient(ORCHESTRATOR);
  }

  @After
  public void removeUserAndCleanPluginProperties() throws Exception {
    Optional<Users.User> user = userRule.getUserByLogin(USER_LOGIN);
    if (user.isPresent()) {
      userRule.deactivateUsers(user.get());
    }
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", null);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.user", null);
  }

  @Test
  public void create_new_user_when_authenticate() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void authenticate_user() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    ORCHESTRATOR.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("authenticate_through_ui",
      "/authorisation/BaseIdentityProviderTest/authenticate_user.html"
    ).build());

    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void display_unauthorized_page_when_authentication_failed() throws Exception {
    enablePlugin();
    // As this property is null, the plugin will throw an exception
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.user", null);

    ORCHESTRATOR.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("display_unauthorized_page_when_authentication_failed",
      "/authorisation/BaseIdentityProviderTest/display_unauthorized_page_when_authentication_failed.html"
    ).build());

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  @Ignore("Do not understand why it's failing...")
  public void fail_to_authenticate_when_not_allowed_to_sign_up() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.allowsUsersToSignUp", "false");

    ORCHESTRATOR.executeSelenese(Selenese.builder().setHtmlTestsInClasspath("fail_to_authenticate_when_not_allowed_to_sign_up",
      "/authorisation/BaseIdentityProviderTest/fail_to_authenticate_when_not_allowed_to_sign_up.html"
    ).build());

    userRule.verifyUserDoesNotExist(USER_LOGIN);
  }

  @Test
  public void update_existing_user_when_authenticate() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME_UPDATED, USER_EMAIL_UPDATED);

    // Second connection, user should be updated
    authenticateWithFakeAuthProvider();

    userRule.verifyUserExists(USER_LOGIN, USER_NAME_UPDATED, USER_EMAIL_UPDATED);
  }

  @Test
  public void reactivate_disabled_user() throws Exception {
    enablePlugin();
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // First connection, user is created
    authenticateWithFakeAuthProvider();

    Optional<Users.User> user = userRule.getUserByLogin(USER_LOGIN);
    assertThat(user).isPresent();

    // Disable user
    userRule.deactivateUsers(user.get());

    // Second connection, user is reactivated
    authenticateWithFakeAuthProvider();
    userRule.verifyUserExists(USER_LOGIN, USER_NAME, USER_EMAIL);
  }

  @Test
  public void not_authenticate_when_plugin_is_disabled() throws Exception {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", "false");
    setUserCreatedByAuthPlugin(USER_LOGIN, USER_PROVIDER_ID, USER_NAME, USER_EMAIL);

    authenticateWithFakeAuthProvider();

    // User is not created as nothing plugin is disabled
    userRule.verifyUserDoesNotExist(USER_LOGIN);

    // TODO Add Selenium test to check login form
  }

  private static void setUserCreatedByAuthPlugin(String login, String providerId, String name, String email) {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.user", login + "," + providerId + "," + name + "," + email);
  }

  private static void authenticateWithFakeAuthProvider() {
    WsResponse response = adminWsClient.wsConnector().call(
      new GetRequest(("/sessions/init/" + FAKE_PROVIDER_KEY)));
    assertThat(response.code()).isEqualTo(200);
  }

  private static void enablePlugin() {
    setServerProperty(ORCHESTRATOR, "sonar.auth.fake-base-id-provider.enabled", "true");
  }

}
