package net.mineguild.minecraft.treedestroyage.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import net.mineguild.minecraft.treedestroyage.TreeDestroyage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.service.sql.SqlService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class BlockLogger {

  private final TreeDestroyage plugin;

  public BlockLogger(TreeDestroyage plugin) {
    this.plugin = plugin;
    try {
      createScheme();
    } catch (SQLException e) {
      e.printStackTrace();
    }

  }

  private SqlService sql;
  private final String dataSource = "jdbc:h2:./config/treedestroyage/treedestroyage_blocklog.db";


  private DataSource getDataSource(String jdbcUrl) throws SQLException {
    if (sql == null) {
      sql = Sponge.getServiceManager().provide(SqlService.class).get();
    }
    return sql.getDataSource(jdbcUrl);
  }

  private void createScheme() throws SQLException {
    Connection conn = getDataSource(dataSource).getConnection();
    PreparedStatement ps = conn.
        prepareStatement(
            "CREATE TABLE IF NOT EXISTS placed (ID INT NOT NULL AUTO_INCREMENT, WORLDNAME varchar(255), X INT, Y INT, Z INT, EXPIRE LONG, PRIMARY KEY (ID));");
    ps.executeUpdate();
    conn.commit();
    conn.close();
  }

  public void insertBlock(BlockSnapshot snap, long expire) throws SQLException {
    Optional<Location<World>> loc = snap.getLocation();
    if (loc.isPresent()) {
      World world = loc.get().getExtent();
      Connection conn = getDataSource(dataSource).getConnection();
      PreparedStatement ps = conn
          .prepareStatement(
              "INSERT INTO placed (WORLDNAME, X, Y, Z, EXPIRE) VALUES (?, ?, ?, ?, ?);");
      ps.setString(1, world.getName());
      ps.setInt(2, loc.get().getBlockX());
      ps.setInt(3, loc.get().getBlockY());
      ps.setInt(4, loc.get().getBlockZ());
      ps.setLong(5, expire);
      ps.executeUpdate();
      conn.close();
    }
  }

  public boolean isPlayerPlaced(Location<World> loc) {
    World world = loc.getExtent();
    try {
      Connection conn = getDataSource(dataSource).getConnection();
      PreparedStatement ps = conn
          .prepareStatement(
              "SELECT EXPIRE FROM placed WHERE WORLDNAME = ? AND X = ? AND Y = ? AND Z = ?;");
      ps.setString(1, world.getName());
      ps.setInt(2, loc.getBlockX());
      ps.setInt(3, loc.getBlockY());
      ps.setInt(4, loc.getBlockZ());
      ResultSet res = ps.executeQuery();

      if (res.next()) {
        conn.close();
        return true;
      } else {
        conn.close();
        return false;
      }
    } catch (SQLException e) {
      plugin.getLogger().error("Unable to request from db!", e);
      return false;
    }

  }

  public void removeBlock(Location<World> loc) {
    World world = loc.getExtent();
    try {
      Connection conn = getDataSource(dataSource).getConnection();
      PreparedStatement ps = conn.prepareStatement(
          "DELETE FROM placed WHERE WORLDNAME = ? AND X = ? AND Y = ? AND Z = ?;");
      ps.setString(1, world.getName());
      ps.setInt(2, loc.getBlockX());
      ps.setInt(3, loc.getBlockY());
      ps.setInt(4, loc.getBlockZ());
      ps.executeUpdate();
      conn.commit();
      conn.close();
    } catch (SQLException e) {
      plugin.getLogger().error("Unable to update db!", e);
    }
  }

  @Listener
  public void handleWorldCreation(LoadWorldEvent event) {
    String worldName = event.getTargetWorld().getName();
    try {
      Connection conn = getDataSource(dataSource).getConnection();
      PreparedStatement ps = conn.
          prepareStatement(
              "CREATE TABLE IF NOT EXISTS ?(ID INT PRIMARY KEY, X INT, Y INT, Z INT, EXPIRE LONG);");
      ps.setString(1, worldName);
      ps.executeUpdate();
      conn.commit();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean isEnabled() {
    return plugin.getConfig().getNode("logPlayerBlocks").getBoolean();
  }


}
