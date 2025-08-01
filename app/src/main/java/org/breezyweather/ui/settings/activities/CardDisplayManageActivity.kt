/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.settings.activities

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.os.Bundle
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Px
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.breezyweather.R
import org.breezyweather.common.basic.BreezyActivity
import org.breezyweather.common.basic.models.options.appearance.CardDisplay
import org.breezyweather.common.bus.EventBus
import org.breezyweather.common.extensions.doOnApplyWindowInsets
import org.breezyweather.common.extensions.getThemeColor
import org.breezyweather.common.utils.ColorUtils
import org.breezyweather.databinding.ActivityCardDisplayManageBinding
import org.breezyweather.domain.settings.SettingsChangedMessage
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.common.adapters.TagAdapter
import org.breezyweather.ui.common.decorations.GridMarginsDecoration
import org.breezyweather.ui.common.decorations.ListDecoration
import org.breezyweather.ui.common.widgets.slidingItem.SlidingItemTouchCallback
import org.breezyweather.ui.settings.adapters.CardDisplayAdapter

class CardDisplayManageActivity : BreezyActivity() {
    private lateinit var mBinding: ActivityCardDisplayManageBinding
    private lateinit var mCardDisplayAdapter: CardDisplayAdapter
    private var mCardDisplayItemTouchHelper: ItemTouchHelper? = null
    private var mTagAdapter: TagAdapter? = null
    private var mBottomAnimator: AnimatorSet? = null
    private var mBottomBarVisibility: Boolean? = null

    @Px
    private var mElevation = 0

    private inner class CardTag(
        var tag: CardDisplay,
    ) : TagAdapter.Tag {
        override val name = tag.getName(this@CardDisplayManageActivity)
    }

    private inner class CardDisplaySwipeCallback : SlidingItemTouchCallback() {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            mCardDisplayAdapter.moveItem(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int,
        ) {
            mCardDisplayAdapter.removeItem(viewHolder.bindingAdapterPosition)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            ViewCompat.setElevation(
                viewHolder.itemView,
                (if (dY != 0f || isCurrentlyActive) mElevation else 0).toFloat()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityCardDisplayManageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mElevation = resources.getDimensionPixelSize(R.dimen.touch_rise_z)
        mBinding.appBar.injectDefaultSurfaceTintColor()
        mBinding.toolbar.setBackgroundColor(
            ColorUtils.getWidgetSurfaceColor(
                6f,
                getThemeColor(androidx.appcompat.R.attr.colorPrimary),
                getThemeColor(com.google.android.material.R.attr.colorSurface)
            )
        )
        mBinding.toolbar.setNavigationOnClickListener { finish() }
        val displayTags = SettingsManager.getInstance(this).cardDisplayList
        mCardDisplayAdapter = CardDisplayAdapter(
            displayTags.toMutableList(),
            { cardDisplay: CardDisplay ->
                setResult(RESULT_OK)
                mTagAdapter!!.insertItem(CardTag(cardDisplay))
                resetBottomBarVisibility()
            },
            { holder: CardDisplayAdapter.ViewHolder ->
                mCardDisplayItemTouchHelper!!.startDrag(holder)
            }
        )
        mBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        mBinding.recyclerView.doOnApplyWindowInsets { view, insets ->
            view.updatePadding(
                left = insets.left,
                right = insets.right
            )
        }
        mBinding.recyclerView.addItemDecoration(
            ListDecoration(
                this,
                getThemeColor(com.google.android.material.R.attr.colorOutline)
            )
        )
        mBinding.recyclerView.adapter = mCardDisplayAdapter
        mCardDisplayItemTouchHelper = ItemTouchHelper(CardDisplaySwipeCallback()).apply {
            attachToRecyclerView(mBinding.recyclerView)
        }
        val otherTags = CardDisplay.entries.toMutableList()
        for (i in otherTags.indices.reversed()) {
            for (displayCard in displayTags) {
                if (otherTags[i] === displayCard) {
                    otherTags.removeAt(i)
                    break
                }
            }
        }
        val tagList = mutableListOf<TagAdapter.Tag>()
        for (tag in otherTags) {
            tagList.add(CardTag(tag))
        }
        mTagAdapter = TagAdapter(
            tagList,
            { _: Boolean, _: Int, newPosition: Int ->
                setResult(RESULT_OK)
                val tag = mTagAdapter!!.removeItem(newPosition) as CardTag
                mCardDisplayAdapter.insertItem(tag.tag)
                resetBottomBarVisibility()
                true
            }
        )
        mBinding.bottomRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        mBinding.bottomRecyclerView.doOnApplyWindowInsets { view, insets ->
            view.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = insets.bottom
            )
        }

        mBinding.bottomRecyclerView.addItemDecoration(
            GridMarginsDecoration(resources.getDimension(R.dimen.normal_margin), mBinding.bottomRecyclerView)
        )
        mBinding.bottomRecyclerView.adapter = mTagAdapter
        mBottomAnimator = null
        mBottomBarVisibility = false
        mBinding.bottomRecyclerView.post { resetBottomBarVisibility() }
    }

    override fun onStop() {
        super.onStop()
        val oldList = SettingsManager.getInstance(this).cardDisplayList
        val newList = mCardDisplayAdapter.cardDisplayList
        if (oldList != newList) {
            SettingsManager.getInstance(this).cardDisplayList = newList
            // We need to do this manually, because the setter above can’t do it, or the main screen would reload
            // on main screen drap & drops
            EventBus
                .instance
                .with(SettingsChangedMessage::class.java)
                .postValue(SettingsChangedMessage())
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        // do nothing.
    }

    private fun resetBottomBarVisibility() {
        val visible = mTagAdapter!!.itemCount != 0
        if (mBottomBarVisibility == null || mBottomBarVisibility != visible) {
            mBottomBarVisibility = visible
            mBottomAnimator?.cancel()
            mBottomAnimator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(
                        mBinding.bottomBar,
                        "alpha",
                        mBinding.bottomBar.alpha,
                        (if (visible) 1 else 0).toFloat()
                    ),
                    ObjectAnimator.ofFloat(
                        mBinding.bottomBar,
                        "translationY",
                        mBinding.bottomBar.translationY,
                        (if (visible) 0 else mBinding.bottomBar.measuredHeight).toFloat()
                    )
                )
                duration = (if (visible) 350 else 150).toLong()
                interpolator = if (visible) DecelerateInterpolator(2f) else AccelerateInterpolator(2f)
            }.also { it.start() }
        }
    }
}
