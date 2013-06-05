package net.bicou.redmine.platform;

import android.accounts.Account;
import android.content.Context;
import net.bicou.redmine.data.Server;
import net.bicou.redmine.data.json.IssueCategory;
import net.bicou.redmine.data.json.Project;
import net.bicou.redmine.data.json.Version;
import net.bicou.redmine.data.sqlite.IssueCategoriesDbAdapter;
import net.bicou.redmine.data.sqlite.ProjectsDbAdapter;
import net.bicou.redmine.data.sqlite.VersionsDbAdapter;
import net.bicou.redmine.util.L;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class ProjectManager {
	public static synchronized long updateProjects(final Context context, final Account account, final Server server, final List<Project> remoteList,
			final long lastSyncMarker) {
		L.d("ctx=" + context + ", account=" + account + ", remote issues count=" + remoteList.size() + " syncMarker=" + lastSyncMarker);

		// Get local project list
		final ProjectsDbAdapter db = new ProjectsDbAdapter(context);
		db.open();
		final List<Project> localProjects = db.selectAll();

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
				db.insert(project);
			} else {
				// Save this as the most recent server update
				projectUpdateDate = project.updated_on.getTimeInMillis();
				if (projectUpdateDate > currentSyncMarker) {
					currentSyncMarker = projectUpdateDate;
				}

				// Project is outdated, update it
				if (project.updated_on.after(lastKnownChange)) {
					db.update(project);
				}
				// Project is up to date
				else {
				}

				localProjects.remove(localProject);
			}
		}

		// Removed / archived projects?
		for (final Project p : localProjects) {
			db.delete(server, p.id);
		}
		db.close();

		return currentSyncMarker;
	}

	public static synchronized long updateVersions(final Context context, final Account account, final Server server, final List<Version> remoteList,
			final long lastSyncMarker) {
		L.d("ctx=" + context + ", account=" + account + ", remote issues count=" + remoteList.size() + " syncMarker=" + lastSyncMarker);

		final VersionsDbAdapter db = new VersionsDbAdapter(context);
		db.open();
		db.deleteAll(server, remoteList.get(0).project.id);
		for (final Version v : remoteList) {
			db.insert(server, v);
		}
		db.close();
		return new GregorianCalendar().getTimeInMillis();
	}

	public static synchronized long updateIssueCategories(Context context, Server server, List<IssueCategory> remoteList, long lastSyncMarker) {
		IssueCategoriesDbAdapter db = new IssueCategoriesDbAdapter(context);
		db.open();
		db.deleteAll(server);
		for (IssueCategory cat : remoteList) {
			cat.server = server;
			db.insert(cat);
		}
		db.close();

		return new GregorianCalendar().getTimeInMillis();
	}
}
