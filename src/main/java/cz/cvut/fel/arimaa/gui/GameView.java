package cz.cvut.fel.arimaa.gui;

import cz.cvut.fel.arimaa.model.Board;
import cz.cvut.fel.arimaa.types.Piece;
import cz.cvut.fel.arimaa.types.Square;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

class GameView extends BorderPane {

    private GameController controller;
    private BoardView boardView;

    GameView() {
        super();
        this.controller = new GameController(this);
        this.boardView = new BoardView();
        setCenter(boardView);
    }

    void update(Board board) {
        boardView.update(board);
    }

    private class BoardView extends GridPane {

        private Map<String, Image> images;
        private ImageView[][] tiles;

        BoardView() {
            super();
            images = new HashMap<>();
            tiles = new ImageView[Board.WIDTH][Board.HEIGHT];
            addBackground();
            addTiles();
        }

        void addBackground() {
            Image image = new Image(getClass().getResourceAsStream("Board.jpg"));
            BackgroundSize size = new BackgroundSize(getWidth(), getHeight(),
                    true, true, true, false);
            BackgroundImage backgroundImage = new BackgroundImage(image,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.DEFAULT, size);
            setBackground(new Background(backgroundImage));
        }

        void addTiles() {
            for (int y = 0; y < Board.HEIGHT; ++y) {
                for (int x = 0; x < Board.WIDTH; ++x) {
                    ImageView tile = new ImageView();
                    tiles[x][y] = tile;
                    add(tile, x, y);

                    int col = x;
                    int row = y;
                    tile.setOnMouseClicked(e -> {
                        controller.onSquareSelected(Square.getSquare(col, row));
                    });
                }
            }
        }

        Image getPieceImage(Piece piece) {
            if (piece == null) {
                return null;
            }

            String url = piece.toString() + ".svg";
            if (!images.containsKey(url)) {
                images.put(url, new Image(getClass().getResourceAsStream(url)));
            }

            return images.get(url);
        }

        void update(Board board) {
            for (int y = 0; y < Board.HEIGHT; ++y) {
                for (int x = 0; x < Board.WIDTH; ++x) {
                    Piece piece = board.getPieceAt(Square.getSquare(x, y));
                    tiles[x][y].setImage(getPieceImage(piece));
                }
            }
        }
    }
}
