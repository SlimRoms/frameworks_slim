/*
 * Copyright (C) 2017 SlimRoms Project
 * Author: Lars Greiss - email: kufikugel@googlemail.com
 * Copyright (C) 2017 ABC rom
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.systemui.slimrecent;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import com.android.systemui.R;

public class ExpandableCardAdapter
        extends RecyclerView.Adapter<ExpandableCardAdapter.ViewHolder> {

    private Context mContext;

    private boolean mFastMode;

    private ArrayList<ExpandableCard> mCards = new ArrayList<>();

    public ExpandableCardAdapter(Context context, boolean fastMode) {
        mContext = context;
        mFastMode = fastMode;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext)
                .inflate(R.layout.card, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        ExpandableCard card = mCards.get(position);
        card.index = position;
        holder.setCard(card);

        holder.screenshot.setVisibility(card.expanded ? View.VISIBLE : View.GONE);
        holder.expandButton.setRotation(card.expanded ? -180 : 0);

        holder.card.setRadius(card.cornerRadius);

        if (card.pinAppIcon) {
            holder.expandButton.setImageDrawable(card.custom);
        } else if (mFastMode) {
            holder.expandButton.setImageResource(R.drawable.ic_options);
        } else if (card.expandVisible) {
            holder.expandButton.setImageResource(R.drawable.ic_expand);
        }
        holder.expandButton.setVisibility(card.noIcon ? View.INVISIBLE : View.VISIBLE);

        if (card.cardBackgroundColor != 0) {
            // we need to override tint list instead of setting cardview background color
            // because some dark themes could change system colors being used by
            // cardview code to set default ColorStateList.
            ColorStateList cl = ColorStateList.valueOf(card.cardBackgroundColor);
            holder.card.setBackgroundTintList(cl);
            holder.optionsView.setBackgroundTintList(cl);
            holder.optionsView.setBackgroundColor(0xff000000 | card.cardBackgroundColor);
            int color;
            if (ColorUtils.isDarkColor(card.cardBackgroundColor)) {
                color = mContext.getColor(R.color.recents_task_bar_light_text_color);
            } else {
                color = mContext.getColor(R.color.recents_task_bar_dark_text_color);
            }
            holder.appName.setTextColor(color);
            holder.expandButton.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            card.textColor = color;
        }

        if (card.appIcon != null) {
            holder.appIcon.setImageDrawable(card.appIcon);
        } else {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        holder.favorite.setVisibility(card.favorite ? View.VISIBLE : View.GONE);

        holder.appName.setText(card.appName);

        if (!mFastMode && card.screenshot != null && !card.screenshot.isRecycled()) {
            holder.screenshot.setImageBitmap(card.screenshot);
        }

        if (!mFastMode && card.needsThumbLoading) {
            card.laterLoadTaskThumbnail();
        }
    }

    public void addCard(ExpandableCard card) {
        mCards.add(card);
        notifyItemInserted(mCards.indexOf(card));
    }

    public void removeCard(int pos)  {
        mCards.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, getItemCount());
    }

    public void clearCards() {
        mCards.clear();
    }

    public ExpandableCard getCard(int pos) {
        return mCards.get(pos);
    }

    public void removeCard(ExpandableCard card) {
        removeCard(mCards.indexOf(card));
    }

    @Override
    public int getItemCount() {
        return mCards.size();
    }

    public void setFastMode(boolean fast) {
        mFastMode = fast;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView screenshot;
        ImageView appIcon;
        ImageView favorite;
        TextView appName;
        ImageView expandButton;
        CardView card;
        LinearLayout cardContent;
        LinearLayout optionsView;
        ExpandableCard expCard;

        private int upX;
        private int upY;

        public ViewHolder(View itemView) {
            super(itemView);
            cardContent = (LinearLayout) itemView.findViewById(R.id.card_content);
            favorite = (ImageView) itemView.findViewById(R.id.favorite_icon);
            appName = (TextView) itemView.findViewById(R.id.app_name);
            appName.setTypeface(Typeface.create(
                    "sans-serif-condensed", Typeface.BOLD));
            screenshot = (ImageView) itemView.findViewById(R.id.screenshot);
            card = (CardView) itemView.findViewById(R.id.card);
            optionsView = (LinearLayout) itemView.findViewById(R.id.card_options);

            appIcon = (ImageView) itemView.findViewById(R.id.app_icon);
            appIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (expCard.appIconClickListener != null) {
                        expCard.appIconClickListener.onClick(v);
                    }
                }
            });
            appIcon.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (expCard.appIconLongClickListener != null) {
                        expCard.appIconLongClickListener.onLongClick(v);
                    }
                    return true;
                }
            });

            expandButton = (ImageView) itemView.findViewById(R.id.expand);
            expandButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (expCard.pinAppIcon) {
                        if (expCard.pinAppListener != null) {
                            expCard.pinAppListener.onClick(v);
                        }
                    } else if (mFastMode) {
                        showOptions();
                    } else if (expCard.expandVisible) {
                        expCard.expanded = !expCard.expanded;
                        if (expCard.expandListener != null) {
                            expCard.expandListener.onExpanded(expCard.expanded);
                        }
                        Fade trans = new Fade();
                        trans.setDuration(150);
                        TransitionManager.beginDelayedTransition(
                                (ViewGroup) itemView.getParent(), trans);
                        expandButton.animate().rotation(expCard.expanded ? -180 : 0);
                        notifyItemChanged(getAdapterPosition());
                    }
                }
            });

            itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    upX = (int) event.getRawX();
                    upY = (int) event.getRawY();
                    return false;
                }
            });
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (expCard.cardClickListener != null) {
                        expCard.cardClickListener.onClick(v);
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showOptions();
                    return true;
                }
            });

            optionsView.setVisibility(View.GONE);
        }

        public void setCard(ExpandableCard card) {
            this.expCard = card;
        }

        void showOptions() {
            // hide other cards options
            for (int i = 0; i < getItemCount(); i++) {
                mCards.get(i).forceHideOptions();
            }
            LayoutInflater inflater = LayoutInflater.from(mContext);
            optionsView.removeAllViewsInLayout();
            for (int i = 0; i < expCard.mOptions.size(); i++) {
                OptionsItem item = expCard.mOptions.get(i);
                ImageView option = (ImageView) inflater.inflate(
                        R.layout.options_item, optionsView, false);
                item.icon.setColorFilter(expCard.textColor, PorterDuff.Mode.MULTIPLY);
                option.setImageDrawable(item.icon);
                option.setId(item.id);
                    option.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (item.clickListener != null) {
                                hideOptions(true);
                                item.clickListener.onClick(v);
                            } else {
                                //finishIcon
                                hideOptions(false);
                            }
                        }
                    });
                optionsView.addView(option);
            }

            optionsView.setVisibility(View.VISIBLE);
            AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
            Interpolator interpolator = AnimationUtils.loadInterpolator(
                    mContext, android.R.interpolator.decelerate_quad);
            animation.setInterpolator(interpolator);
            animation.setDuration(350);
            optionsView.startAnimation(animation);
            cardContentVisibility(false);
        }

        public void hideOptions(boolean force) {
            if (force) {
                optionsView.setVisibility(View.GONE);
                cardContentVisibility(true);
                return;
            }
            AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
            Interpolator interpolator = AnimationUtils.loadInterpolator(
                    mContext, android.R.interpolator.accelerate_quad);
            animation.setInterpolator(interpolator);
            animation.setDuration(300);
            animation.setAnimationListener(animListener);
            optionsView.startAnimation(animation);
            cardContentVisibility(true);
        }

        void cardContentVisibility(boolean show) {
            appIcon.setVisibility(show ? View.VISIBLE : View.GONE);
            appName.setVisibility(show ? View.VISIBLE : View.GONE);
            favorite.setVisibility(show && expCard.favorite ? View.VISIBLE : View.GONE);
            expandButton.setVisibility(show && !expCard.noIcon ? View.VISIBLE : View.GONE);
        }

        Animation.AnimationListener animListener =
                new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                optionsView.setVisibility(View.GONE);
            }
            public void onAnimationRepeat(Animation animation) {
            }
            public void onAnimationStart(Animation animation) {
            }
        };
    }

    public interface ExpandListener {
        void onExpanded(boolean expanded);
    }

    public interface RefreshListener {
        void onRefresh(int index);
    }

    public interface HideOptionsListener {
        void onHideOptions(int index);
    }

    public static class ExpandableCard {
        boolean expanded = false;
        String appName;
        Drawable appIcon;
        Bitmap screenshot;
        private ArrayList<OptionsItem> mOptions = new ArrayList<>();
        int textColor;
        boolean expandVisible = true;
        boolean pinAppIcon = false;
        boolean noIcon = false;
        boolean favorite = false;
        Context context;
        String identifier;
        float scaleFactor;
        int thumbnailWidth;
        int thumbnailHeight;
        boolean needsThumbLoading = false;
        int index;
        void laterLoadTaskThumbnail() {
            RecentPanelView.laterLoadTaskThumbnail(
                    context, this, identifier, scaleFactor,
                    thumbnailWidth, thumbnailHeight, persistentTaskId);
        }
        float cornerRadius;
        View.OnClickListener appIconClickListener;
        View.OnClickListener pinAppListener;
        View.OnLongClickListener appIconLongClickListener;
        int cardBackgroundColor;
        Drawable custom;
        View.OnClickListener cardClickListener;
        ExpandListener expandListener;
        RefreshListener refreshListener;
        HideOptionsListener hideOptionsListener;
        int persistentTaskId = -1;
        String packageName;

        public ExpandableCard(String appName, Drawable appIcon) {
            this.appName = appName;
            this.appIcon = appIcon;
        }

        public void addOption(OptionsItem item) {
            mOptions.add(item);
        }

        public void clearOptions() {
            mOptions.clear();
        }

        void refreshThumb() {
            refreshListener.onRefresh(index);
        }

        void forceHideOptions() {
            hideOptionsListener.onHideOptions(index);
        }
    }

    public static class OptionsItem {
        int id;
        Drawable icon;
        View.OnClickListener clickListener;
        boolean finishIcon = false;

        public OptionsItem(Drawable icon, int id,
                View.OnClickListener clickListener) {
            this.icon = icon;
            this.id = id;
            this.clickListener = clickListener;
        }

        public OptionsItem(Drawable icon, int id, boolean finish) {
            this(icon, id, null);
            this.finishIcon = finish;
        }
    }
}
