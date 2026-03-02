package server;

import model.GameData;
import java.util.List;

public record GamesResponse(List<GameData> games) {

}
