package Utils;

import java.io.Serializable;

public class ArtistName implements Serializable {

	private static final long serialVersionUID = 1285132875800819988L;
	private String artistName;

	public ArtistName(String artistName){ this.artistName = artistName; }

	public String getArtistName() { return artistName; }
	public void setArtistName(String artistName) { this.artistName = artistName; }
}