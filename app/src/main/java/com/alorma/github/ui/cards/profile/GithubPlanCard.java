package com.alorma.github.ui.cards.profile;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alorma.github.R;
import com.alorma.github.sdk.bean.dto.response.User;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.octicons_typeface_library.Octicons;

/**
 * Created by Bernat on 23/11/2014.
 */
public class GithubPlanCard implements View.OnClickListener {

    private GithubDataCardListener githubDataCardListener;

    private User user;
    private int avatarColor;

    public GithubPlanCard(User user, View view, int avatarColor) {
        this.user = user;
        this.avatarColor = avatarColor;
        setupInnerViewElements(view);
    }

    public void setupInnerViewElements(View view) {

        if (user.plan != null) {
            setUpName(view);
            setUpQuota(view);
            setUpCollaborators(view);
            setUpRepos(view);
        }
    }

    private void setUpName(View view) {
        if (user.plan.name != null) {
            TextView text = (TextView) view.findViewById(R.id.textPlanName);

            text.setText(user.plan.name);
        } else {
            view.findViewById(R.id.planName).setVisibility(View.GONE);
            view.findViewById(R.id.dividerPlanName).setVisibility(View.GONE);
        }
    }

    private void setUpQuota(View view) {
        ImageView icon = (ImageView) view.findViewById(R.id.iconPlanQuota);

        IconicsDrawable githubIconDrawable = drawable(view.getContext(), Octicons.Icon.oct_database);

        icon.setImageDrawable(githubIconDrawable);

        TextView text = (TextView) view.findViewById(R.id.textPlanQuota);

        text.setText(view.getContext().getString(R.string.quota_data, humanReadableByteCount(user.plan.space, true)));
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private void setUpCollaborators(View view) {
        ImageView icon = (ImageView) view.findViewById(R.id.iconPlanCollaborators);

        IconicsDrawable githubIconDrawable = drawable(view.getContext(), Octicons.Icon.oct_organization);

        icon.setImageDrawable(githubIconDrawable);

        TextView text = (TextView) view.findViewById(R.id.textPlanCollaborators);

        text.setText(view.getContext().getString(R.string.collaborators_data, user.plan.collaborators));

    }

    private void setUpRepos(View view) {
        ImageView icon = (ImageView) view.findViewById(R.id.iconPlanRepos);

        IconicsDrawable githubIconDrawable = drawable(view.getContext(), Octicons.Icon.oct_repo_forked);

        icon.setImageDrawable(githubIconDrawable);

        TextView text = (TextView) view.findViewById(R.id.textPlanRepos);

        text.setText(view.getContext().getString(R.string.repos_data_profile, user.total_private_repos, user.plan.privateRepos));

    }

    private IconicsDrawable drawable(Context context, Octicons.Icon icon) {
        IconicsDrawable githubIconDrawable = new IconicsDrawable(context, icon);

        githubIconDrawable.sizeDp(30);
        githubIconDrawable.color(avatarColor);

        return githubIconDrawable;
    }

    @Override
    public void onClick(View v) {
        if (githubDataCardListener != null) {
            switch (v.getId()) {
                case R.id.repositories:
                    githubDataCardListener.onRepositoriesRequest(user.login);
                    break;
            }
        }
    }

    public void setGithubDataCardListener(GithubDataCardListener githubDataCardListener) {
        this.githubDataCardListener = githubDataCardListener;
    }

    public interface GithubDataCardListener {
        void onRepositoriesRequest(String username);
    }
}
