/**
 * The MIT License (MIT)
 * Copyright (c) 2012 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package us.nineworlds.serenity.ui.browser.tv.episodes;

import android.app.Activity;
import android.content.Context;
import android.provider.MediaStore;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.birbit.android.jobqueue.JobManager;
import com.bumptech.glide.Glide;

import net.ganin.darv.DpadAwareRecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import javax.inject.Inject;

import us.nineworlds.plex.rest.model.impl.MediaContainer;
import us.nineworlds.serenity.R;
import us.nineworlds.serenity.core.model.VideoContentInfo;
import us.nineworlds.serenity.core.model.impl.EpisodeMediaContainer;
import us.nineworlds.serenity.events.EpisodesRetrievalEvent;
import us.nineworlds.serenity.events.TVShowRetrievalEvent;
import us.nineworlds.serenity.jobs.EpisodesRetrievalJob;
import us.nineworlds.serenity.ui.adapters.AbstractPosterImageGalleryAdapter;
import us.nineworlds.serenity.ui.util.ImageUtils;

/**
 * Implementation of the Poster Image Gallery class for TV Shows.
 *
 * @author dcarver
 */
public class EpisodePosterImageGalleryAdapter extends AbstractPosterImageGalleryAdapter {

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.poster_indicator_view, null);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        VideoContentInfo pi = posterList.get(position);
        ImageView mpiv = (ImageView) holder.itemView
                .findViewById(R.id.posterImageView);
        mpiv.setBackgroundResource(R.drawable.gallery_item_background);
        mpiv.setScaleType(ImageView.ScaleType.FIT_XY);
        int width = ImageUtils.getDPI(300, (Activity) mpiv.getContext());
        int height = ImageUtils.getDPI(187, (Activity) mpiv.getContext());
        mpiv.setMaxHeight(height);
        mpiv.setMaxWidth(width);
        mpiv.setLayoutParams(new RelativeLayout.LayoutParams(width, height));
        holder.itemView.setLayoutParams(new DpadAwareRecyclerView.LayoutParams(width,
                height));

        Glide.with(mpiv.getContext()).load(pi.getImageURL()).into(mpiv);

        setWatchedStatus(holder.itemView, pi);
    }

    public void updateEpisodes(List<VideoContentInfo> episodes) {
        final List<VideoContentInfo> oldList = posterList;
        final List<VideoContentInfo> newList = episodes;

        posterList = episodes;

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = oldList.get(oldItemPosition).id();
                String newId = newList.get(newItemPosition).id();
                return oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                VideoContentInfo old = oldList.get(oldItemPosition);
                VideoContentInfo newContent = newList.get(newItemPosition);
                return old.equals(newContent);
            }
        });

        diffResult.dispatchUpdatesTo(this);
    }

    public class EpisodeViewHolder extends DpadAwareRecyclerView.ViewHolder {

        public EpisodeViewHolder(View itemView) {
            super(itemView);
        }
    }

}
