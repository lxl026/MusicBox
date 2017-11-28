package company.leon.musicbox;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by Leon on 2017/11/20.
 */

public abstract  class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<BaseRecyclerHolder> {

    private Context context;//上下文
    private List<T> list;//数据源
    private LayoutInflater inflater;//布局器
    private int itemLayoutId;//布局id
    private boolean isScrolling;//是否在滚动
    private OnItemClickListener listener;//点击事件监听器
    private RecyclerView recyclerView;

    //在RecyclerView提供数据的时候调用
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    /**
     * 定义一个点击事件接口回调
     */
    public interface OnItemClickListener {
        void onClick(int position);
        void onLongClick(int position);
    }


    public void delete(int position) {
        list.remove(position);
        notifyItemRemoved(position);
    }

    public BaseRecyclerAdapter(Context context, List<T> list, int itemLayoutId) {
        this.context = context;
        this.list = list;
        this.itemLayoutId = itemLayoutId;
    }

    @Override
    public BaseRecyclerHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        BaseRecyclerHolder viewholder = BaseRecyclerHolder.getRecyclerHolder(context,parent,itemLayoutId);
        return viewholder;
    }

    @Override
    public void onBindViewHolder(final BaseRecyclerHolder holder, int position) {

        if (listener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    listener.onClick(holder.getAdapterPosition());
                }
            });//设置背景
        }


        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                listener.onLongClick(holder.getAdapterPosition());
                return false;
            }
        });

        convert(holder, list.get(position));

    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public T getItem(int i)
    {
        if(list==null) return null;
        return list.get(i);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public abstract void convert(BaseRecyclerHolder holder, T item);
}