/*
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package hudson.plugins.sonar.model;

import hudson.Util;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.plugins.sonar.Messages;
import hudson.triggers.SCMTrigger;
import hudson.triggers.TimerTrigger;

import java.io.Serializable;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Evgeny Mandrikov
 * @since 1.2
 */
public class TriggersConfig implements Serializable {
  private boolean scmBuilds;

  private boolean timerBuilds;

  /**
   * @since 1.2
   */
  private boolean userBuilds;

  private boolean snapshotDependencyBuilds;

  /**
   * @since 1.7
   */
  private String envVar;

  public TriggersConfig() {
  }

  @DataBoundConstructor
  public TriggersConfig(boolean scmBuilds, boolean timerBuilds, boolean userBuilds, boolean snapshotDependencyBuilds, String envVar) {
    this.scmBuilds = scmBuilds;
    this.timerBuilds = timerBuilds;
    this.userBuilds = userBuilds;
    this.snapshotDependencyBuilds = snapshotDependencyBuilds;
    this.envVar = envVar;
  }

  public boolean isScmBuilds() {
    return scmBuilds;
  }

  public void setScmBuilds(boolean scmBuilds) {
    this.scmBuilds = scmBuilds;
  }

  public boolean isTimerBuilds() {
    return timerBuilds;
  }

  public void setTimerBuilds(boolean timerBuilds) {
    this.timerBuilds = timerBuilds;
  }

  public boolean isUserBuilds() {
    return userBuilds;
  }

  public void setUserBuilds(boolean userBuilds) {
    this.userBuilds = userBuilds;
  }

  public boolean isSnapshotDependencyBuilds() {
    return snapshotDependencyBuilds;
  }

  public void setSnapshotDependencyBuilds(boolean snapshotDependencyBuilds) {
    this.snapshotDependencyBuilds = snapshotDependencyBuilds;
  }

  public String getEnvVar() {
    return Util.fixEmptyAndTrim(envVar);
  }

  public void setEnvVar(String envVar) {
    this.envVar = envVar;
  }

  public String isSkipSonar(AbstractBuild<?, ?> build) {
    Result result = build.getResult();

    if (result != null) {
      // skip analysis if build failed
      // unstable means that build completed, but there were some test failures, which is not critical for analysis
      if (result.isWorseThan(Result.UNSTABLE)) {
        return Messages.SonarPublisher_BadBuildStatus(build.getResult().toString());
      }
    }

    if (getEnvVar() != null) {
      String value = build.getBuildVariableResolver().resolve(getEnvVar());
      if ("true".equalsIgnoreCase(value)) {
        return null;
      }
    }
    if (isTrigger(build, SonarCause.class)) {
      return null;
    }
    if (isScmBuilds() && isTrigger(build, SCMTrigger.SCMTriggerCause.class)) {
      return null;
    }
    if (isTimerBuilds() && isTrigger(build, TimerTrigger.TimerTriggerCause.class)) {
      return null;
    }
    if (isUserBuilds() && isTrigger(build, Cause.UserCause.class)) {
      return null;
    }
    if (isSnapshotDependencyBuilds() && isTrigger(build, Cause.UpstreamCause.class)) {
      return null;
    }
    return Messages.Skipping_Sonar_analysis();
  }

  /**
   * Returns true, if specified build triggered by specified trigger.
   *
   * @param build   build
   * @param trigger trigger
   * @return true, if specified build triggered by specified trigger
   */
  private static boolean isTrigger(AbstractBuild<?, ?> build, Class<? extends hudson.model.Cause> trigger) {
    CauseAction buildCause = build.getAction(CauseAction.class);
    List<Cause> buildCauses = buildCause.getCauses();
    for (Cause cause : buildCauses) {
      if (trigger.isInstance(cause)) {
        return true;
      }
    }
    return false;
  }

  /**
   * For internal use only.
   */
  public static class SonarCause extends Cause {
    @Override
    public String getShortDescription() {
      return null;
    }
  }
}