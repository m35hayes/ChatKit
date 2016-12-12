package com.stfalcon.chatkit.features.messages.adapters;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stfalcon.chatkit.R;
import com.stfalcon.chatkit.commons.adapter.ViewHolder;
import com.stfalcon.chatkit.features.messages.adapters.holders.DefaultDateHeaderViewHolder;
import com.stfalcon.chatkit.features.messages.adapters.holders.DefaultIncomingMessageViewHolder;
import com.stfalcon.chatkit.features.messages.adapters.holders.DefaultOutcomingMessageViewHolder;
import com.stfalcon.chatkit.features.messages.adapters.holders.MessageViewHolder;
import com.stfalcon.chatkit.features.messages.models.IMessage;
import com.stfalcon.chatkit.features.utils.DatesUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/*
 * Created by troy379 on 09.12.16.
 */
public class MessagesAdapter<MESSAGE extends IMessage>
        extends RecyclerView.Adapter<ViewHolder> {

    private static final int VIEW_TYPE_INCOMING_MESSAGE = 0x00;
    private static final int VIEW_TYPE_OUTCOMING_MESSAGE = 0x01;
    private static final int VIEW_TYPE_DATE_HEADER = 0x02;

    private HoldersConfig holders;
    private String senderId;
    private List<Wrapper> items;

    public MessagesAdapter(String senderId) {
        this(new HoldersConfig(), senderId);
    }

    public MessagesAdapter(HoldersConfig holders, String senderId) {
        this.holders = holders;
        this.senderId = senderId;
        this.items = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_INCOMING_MESSAGE:
                return ViewHolder.getHolder(parent, holders.incomingLayout, holders.incomingHolder);
            case VIEW_TYPE_OUTCOMING_MESSAGE:
                return ViewHolder.getHolder(parent, holders.outcomingLayout, holders.outcomingHolder);
            case VIEW_TYPE_DATE_HEADER:
                return ViewHolder.getHolder(parent, holders.dateHeaderLayout, holders.dateHeaderHolder);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.onBind(items.get(position).item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        Wrapper wrapper = items.get(position);
        if (wrapper.item instanceof IMessage) {
            IMessage message = (IMessage) wrapper.item;
            if (message.getAuthorId().contentEquals(senderId)) {
                return VIEW_TYPE_OUTCOMING_MESSAGE;
            } else {
                return VIEW_TYPE_INCOMING_MESSAGE;
            }
        } else {
            return VIEW_TYPE_DATE_HEADER;
        }
    }

    /*
    * PRIVATE METHODS
    * */
    public void add(MESSAGE message) {
        boolean isNewMessageToday = !isPreviousSameDate(0, message.getCreatedAt());
        if (isNewMessageToday) {
            items.add(0, new Wrapper<>(message.getCreatedAt()));
        }
        Wrapper<MESSAGE> element = new Wrapper<>(message);
        items.add(0, element);
        notifyItemRangeInserted(0, isNewMessageToday ? 2 : 1);
    }

    public void add(ArrayList<MESSAGE> messages) {
        Collections.reverse(messages);
        int oldSize = items.size();
        generateDateHeaders(messages);
        notifyItemRangeInserted(oldSize, items.size() - oldSize);
    }

    public void update(String oldId, MESSAGE newMessage) {
        int position = getMessagePositionById(oldId);
        Wrapper<MESSAGE> element = new Wrapper<>(newMessage);
        items.set(position, element);
        notifyItemChanged(position);
    }

    public void deleteByIds(String[] ids) {
        for (String id : ids) {
            int index = getMessagePositionById(id);
            items.remove(index);
            notifyItemRemoved(index);
        }
        recountDateHeaders();
    }

    @SuppressWarnings("unchecked")
    public void onLastItemLoaded() {
        MESSAGE lastMessage = (MESSAGE) items.get(items.size() - 1).item;
        this.items.add(new Wrapper<>(lastMessage.getCreatedAt()));
        notifyItemInserted(items.size());
    }

    /*
    * PRIVATE METHODS
    * */
    private void recountDateHeaders() {
        ArrayList<Integer> indicesToDelete = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            Wrapper wrapper = items.get(i);
            if (wrapper.item instanceof Date) {
                if (i == 0) {
                    indicesToDelete.add(i);
                } else {
                    if (items.get(i - 1).item instanceof Date) {
                        indicesToDelete.add(i);
                    }
                }
            }
        }

        Collections.reverse(indicesToDelete);
        for (int i : indicesToDelete) {
            items.remove(i);
            notifyItemRemoved(i);
        }
    }

    private void generateDateHeaders(ArrayList<MESSAGE> messages) {
        for (int i = 0; i < messages.size(); i++) {
            MESSAGE message = messages.get(i);
            this.items.add(new Wrapper<>(message));
            if (messages.size() > i + 1) {
                MESSAGE nextMessage = messages.get(i + 1);
                if (!DatesUtils.isSameDay(message.getCreatedAt(), nextMessage.getCreatedAt())) {
                    this.items.add(new Wrapper<>(message.getCreatedAt()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int getMessagePositionById(String id) {
        for (int i = 0; i < items.size(); i++) {
            Wrapper wrapper = items.get(i);
            if (wrapper.item instanceof IMessage) {
                MESSAGE message = (MESSAGE) wrapper.item;
                if (message.getId().contentEquals(id)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private boolean isPreviousSameDate(int position, Date dateToCompare) {
        if (items.size() <= position) return false;
        if (items.get(position).item instanceof IMessage) {
            Date previousPositionDate = ((MESSAGE) items.get(position).item).getCreatedAt();
            return DatesUtils.isSameDay(dateToCompare, previousPositionDate);
        } else return false;
    }

    @SuppressWarnings("unchecked")
    private boolean isPreviousSameAuthor(String id, int position) {
        int prevPosition = position + 1;
        if (items.size() <= prevPosition) return false;

        if (items.get(prevPosition).item instanceof IMessage) {
            return ((MESSAGE) items.get(prevPosition).item).getAuthorId().contentEquals(id);
        } else return false;
    }

    /*
    * HOLDERS CONFIG
    * */
    public static class HoldersConfig {

        private Class<? extends MessageViewHolder<? extends IMessage>> incomingHolder;
        private @LayoutRes int incomingLayout;

        private Class<? extends MessageViewHolder<? extends IMessage>> outcomingHolder;
        private @LayoutRes int outcomingLayout;

        private Class<? extends ViewHolder<Date>> dateHeaderHolder;
        private @LayoutRes int dateHeaderLayout;

        public HoldersConfig() {
            this.incomingHolder = DefaultIncomingMessageViewHolder.class;
            this.incomingLayout = R.layout.item_incoming_message;

            this.outcomingHolder = DefaultOutcomingMessageViewHolder.class;
            this.outcomingLayout = R.layout.item_outcoming_message;

            this.dateHeaderHolder = DefaultDateHeaderViewHolder.class;
            this.dateHeaderLayout = R.layout.item_date_header;
        }

        public void setIncoming(Class<? extends MessageViewHolder<? extends IMessage>> holder, @LayoutRes int layout) {
            this.incomingHolder = holder;
            this.incomingLayout = layout;
        }

        public void setOutcoming(Class<? extends MessageViewHolder<? extends IMessage>> holder, @LayoutRes int layout) {
            this.outcomingHolder = holder;
            this.outcomingLayout = layout;
        }

        public void setDateHeader(Class<? extends ViewHolder<Date>> holder, @LayoutRes int layout) {
            this.dateHeaderHolder = holder;
            this.dateHeaderLayout = layout;
        }
    }

    /*
    * WRAPPERS
    * */
    private class Wrapper<DATA> {
        private DATA item;

        Wrapper(DATA item) {
            this.item = item;
        }
    }

    /*
    * LISTENERS
    * */
    public interface Listener<MESSAGE extends IMessage> {
        void onMessageLongClick(MESSAGE message);
    }
}
