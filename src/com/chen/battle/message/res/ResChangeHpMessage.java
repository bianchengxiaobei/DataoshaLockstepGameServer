package com.chen.battle.message.res;

import org.apache.mina.core.buffer.IoBuffer;

import com.chen.message.Message;

public class ResChangeHpMessage extends Message
{
	public long playerId;
	public int value;
	public int reason;
	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 1040;
	}

	@Override
	public String getQueue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean read(IoBuffer arg0) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean write(IoBuffer buffer)
	{
		writeLong(buffer, playerId);
		writeInt(buffer, value);
		writeInt(buffer, reason);
		return true;
	}
	
}