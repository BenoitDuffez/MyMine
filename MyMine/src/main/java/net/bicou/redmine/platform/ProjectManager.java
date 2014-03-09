package net.bicou.redmine.platform;

import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.IssueCategory;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Tracker;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.sqlite.DbAdapter;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.TrackersDbAdapter;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.util.L;
import net.bicou.redmine.util.Util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class ProjectManager {
	public static synchronized long updateProjects(final DbAdapter db, final Server server, final List<Project> remoteList, final long lastSyncMarker) {
		L.d("remote issues count=" + remoteList.size() + " syncMarker=" + lastSyncMarker);

		// Get local project list
		final ProjectsDbAdapter pdb = new ProjectsDbAdapter(db);
		pdb.open();
		final List<Project> localProjects = pdb.selectAll();

		// Mark last update date
		long currentSyncMarker = lastSyncMarker;
		long projectUpdateDate;
		Project localProject;
		final Calendar lastKnownChange = new GregorianCalendar();
		lastKnownChange.setTimeInMillis(lastSyncMarker);

		// Loop projects for update
		for (final Project project : remoteList) {
			// Try to get the local copy of that remote project
			localProject = null;
			for (final Project lp : localProjects) {
				if (lp.id == project.id) {
					localProject = lp;
					break;
				}
			}

			project.server = server;

			// New project, add it
			if (localProject == null) {
				pdb.insert(project);
			} else {
				project.is_sync_blocked = localProject.is_sync_blocked;

				// Save this as the most recent server update
				projectUpdateDate = Util.isEpoch(project.updated_on) ? 0 : project.updated_on.getTimeInMillis();
				if (projectUpdateDate > currentSyncMarker) {
					currentSyncMarker = projectUpdateDate;
				}

				// Project is outdated, update it
				if (project.updated_on.after(lastKnownChange)) {
					pdb.update(project);
				}
				// Project is up to date
				else {
				}

				localProjects.remove(localProject);
			}
		}

		// Removed / archived projects?
		for (final Project p : localProjects) {
			pdb.delete(server, p.id);
		}

		return currentSyncMarker;
	}

	public static synchronized long updateVersions(final DbAdapter db, final Server server, final List<Version> remoteList) {
		L.d("remote versions count=" + remoteList.size());

		final VersionsDbAdapter vdb = new VersionsDbAdapter(db);
		vdb.deleteAll(server, remoteList.get(0).project.id);
		for (final Version v : remoteList) {
			vdb.insert(server, v);
		}
		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updateIssueCategories(DbAdapter db, Server server, Project project, List<IssueCategory> remoteList) {
		L.d("remote categories count=" + remoteList.size());

		IssueCategoriesDbAdapter iscdb = new IssueCategoriesDbAdapter(db);
		iscdb.deleteAll(server, project.id);
		for (IssueCategory cat : remoteList) {
			cat.server = server;
			iscdb.insert(cat);
		}

		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updateProjectTrackers(DbAdapter db, Server server, Project project, List<Tracker> trackers) {
		TrackersDbAdapter trackersDbAdapter = new TrackersDbAdapter(db);
		trackersDbAdapter.deleteAll(server, project.id);
		for (Tracker tracker : trackers) {
			trackersDbAdapter.insert(server, project, tracker);
		}
		return new GregorianCalendar().getTimeInMillis();
	}
}
