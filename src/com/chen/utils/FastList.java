package com.chen.utils;


public class FastList
{
	private final int DefaultCapacity = 8;
	public byte[] innerArray;
	public int Capacity = DefaultCapacity;
	public int Count;
	public FastList(FastList copyList)
	{
		innerArray = copyList.innerArray.clone();
		Count = innerArray.length;
		Capacity = innerArray.length;
	}
	public FastList(byte[] start)
	{
		innerArray = start;
		Count = innerArray.length;
		Capacity = innerArray.length;
	}
	public FastList(int startCapacity)
	{
		Capacity = startCapacity;
		innerArray = new byte[Capacity];
		Count = 0;
	}
	public FastList()
	{
		innerArray = new byte[Capacity];
		Count = 0;
	}
	public void CheckCapacity(int min)
	{
		if (Capacity < min)
		{
			Capacity *= 2;
			if (Capacity < min) {
				Capacity = min;
			}
		}
		innerArray = (byte[]) Tools.ResizeArray(innerArray, Capacity);
	}
	public byte Get(int index)
	{
		try {
			return this.innerArray[index];
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}	
	}
	public void AddRange(byte[] data)
	{
		if (data == null)
		{
			System.err.println("数据为空");
			return;
		}
		int len = data.length;
		this.CheckCapacity(Count+len+1);
		for (int i = 0; i < len; i++)
		{
			innerArray[Count++] = data[i];
		}
	}
	public byte[] ToArray()
	{
		if (Count == 0)
		{
			return null;
		}
		byte[] retArray = new byte[Count];
		System.arraycopy (innerArray,0,retArray,0,Count);
		return retArray;
	}
	public void Clear()
	{
		for (int i=0;i<innerArray.length;i++)
		{
			innerArray[i] = 0;
		}
		Count = 0;
	}
	public void FastClear()
	{
		Count = 0;
	}
}
