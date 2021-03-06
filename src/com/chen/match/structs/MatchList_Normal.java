package com.chen.match.structs;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;


public class MatchList_Normal extends MatchList
{
	private ConcurrentHashMap<Integer, MatchRoom_Normal> roomList;
	public MatchList_Normal(int mapId)
	{
		super(mapId);
		roomList = new ConcurrentHashMap<Integer, MatchRoom_Normal>();
	}
	@Override
	public boolean addOneTeam(MatchTeam team)
	{
		Iterator<MatchRoom_Normal> iter = roomList.values().iterator();
		while (iter.hasNext()) {
			MatchRoom_Normal matchRoom_Normal = iter.next();
			if (matchRoom_Normal != null)
			{
				if (matchRoom_Normal.addOneTeam(team))
				{
					return true;
				}
			}
			else
			{
				System.err.println("Room == null");
			}					
		}
		MatchRoom_Normal room = new MatchRoom_Normal(this.mapBean.getM_nMapId());
		boolean success = room.addOneTeam(team);
		roomList.put(room.getRoomId(),room);
		return success;
	}
	@Override
	public boolean removeOneTeam(MatchTeam team)
	{
		for (MatchRoom_Normal room_Normal : roomList.values())
		{
			room_Normal.removeOneTeam(team);
			return true;
		}
		return false;
	}
	@Override
	public void update() {
		MatchRoom_Normal room = null;
		Iterator<MatchRoom_Normal> iter = roomList.values().iterator();
		while (iter.hasNext()) {
			room = iter.next();
			if (room.update())
			{
				//MatchRoom_Normal match = roomList.remove(room.getRoomId());
				//match = null;
				System.out.println("移除匹配房间");
				iter.remove();				
			}
		}
	}
	@Override
	public boolean addInvitePlayer(MatchPlayer player, int matchRoomId,
			boolean isAccept) {
		// TODO Auto-generated method stub
		return false;
	}
}
