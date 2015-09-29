package me.config;

import com.google.common.base.MoreObjects;

/**
 * 当前进程信息
 * Created by lirui on 2015-09-28 20:34.
 */
public class ProcessProperties {
	private String team;
	private String name;
	private String profile;

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProfile() {
		return profile;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("team", team)
				.add("name", name)
				.add("profile", profile)
				.toString();
	}
}
